package sq.waveGenerators;

import glabs.RandomUtility;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import org.newdawn.slick.geom.Ellipse;
import org.newdawn.slick.geom.Shape;
import sq.*;

/**
 * First-generation wave generator.
 * Compute score by accumulative average, considering kills, longetivity (i.e. time on field) and near hit factor (how close it is near
 * to the player)
 * Then generate wave by randomly picking enemies that totals certain score, by precomputing integer that sum to target.
 * @author Peter
 */
public class DefaultGenerator implements WaveGeneratorInterface {
    
    private static class GeneData implements Serializable{
        private static final long serialVersionUID = 1L;
        double score;
        double appearance;
        Set<Gene> connectedGenes;
        public GeneData(double s, double a, Set<Gene> g){
            score = s; appearance = a; connectedGenes = g;
        }
        public GeneData(){
            this(0, 0, new HashSet<Gene>());
        }
    }
    
    private Map<Gene, GeneData> scores = new HashMap<>();
    private boolean elitism = false;
    
    public void loadData(){
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("geneScore.sgn"))){
            scores = (Map<Gene, GeneData>) ois.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException ex){
            scores = new HashMap<>();
        }
    }
    
    public void saveData(){
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("geneScore.sgn"))){
            oos.writeObject(scores);
        } catch (IOException ex){
            //ignore
        }
    }
    
    public void updateGeneScores(Set<AI> oldEnemies, Set<AIGene> allGenes){
        //compute score for individual genes
        for (AI i : oldEnemies) {
            AIGene g;
            AI owner = i;
            if (i.getGene().getLifetime() < 0) {
                g = i.getGene();
            } else {
                do {
                    owner = (AI) owner.getOwner();
                    g = owner.getGene();
                } while (g.getLifetime() > 0);
            }
            GeneData record = scores.get(g);
            if (record == null){
               throw new AssertionError();
            }
            double score = record.score;
            double appearance = record.appearance;
            double thisTurnScore = Math.pow(owner.getKills(), 3) * 5000 + 
                             Math.pow(owner.getLongetivity(), 1.2) / 100 + Math.pow(owner.getNearHitScore(), 2);
            score = (score * appearance + thisTurnScore) / (appearance + 1);
            appearance += 1;
            scores.put(g, new GeneData(score, appearance, scores.get(g).connectedGenes));
            for (Gene j : record.connectedGenes){
                GeneData jRecord = scores.get(j);
                if (jRecord == null){
                    jRecord = new GeneData();
                }
                double jScore = jRecord.score;
                double jAppearance = jRecord.appearance;
                jScore = (jScore * jAppearance + thisTurnScore * 0.5) / (jAppearance + 0.5);
                jAppearance += 0.5;
                scores.put(j, new GeneData(jScore, jAppearance, scores.get(j).connectedGenes));
            }
        }
    }
    
    public void createNewGenes(Set<AIGene> allGenes){   
        //remove some genes if there are a lot.
        if (allGenes.size() > 30){
            while (RandomUtility.randBetween(0, allGenes.size()) > 15){
                double min = Double.MAX_VALUE;
                for (GeneData i : scores.values()){
                    if (i.appearance < min){
                        min = i.appearance;
                    }
                }
                Set<AIGene> minGene = new HashSet<>();
                for (AIGene i : allGenes){
                    if (scores.get(i).score < min * 1.1){
                        minGene.add(i);
                    }
                }
                allGenes.remove(RandomUtility.randomPick(minGene));
            }
        }
        
        Set<AIGene> candidates = new HashSet<>();
        if (!elitism){
            candidates = allGenes;
        } else {
            double max = Double.MIN_VALUE;
            for (GeneData i : scores.values()){
                if (i.score > max){
                    max = i.score;
                }
            }
            for (AIGene i : allGenes){
                if (scores.get(i).score > max * 0.9){
                    candidates.add(i);
                }
            }
        }
        
        //mutate genes and create new ones
        int mutated = 0;
        while (RandomUtility.randBetween(0, allGenes.size()) < 20 || mutated < 1){
            AIGene selectedGene;
            do {
                selectedGene = RandomUtility.randomPick(candidates);
            } while (selectedGene.getLifetime() > 0);
            AIGene resultantGene = (AIGene) selectedGene.mutate(false, false);
            scores.put(resultantGene, new GeneData(scores.get(selectedGene) == null ? 0 : scores.get(selectedGene).score, 0, 
                    new HashSet<Gene>()));
            allGenes.add(resultantGene);
            mutated++;
        }

        //tweak more genes
        /*mutated = 0;
        while (RandomUtility.randBetween(0, allGenes.size()) < 40 || mutated < 1){
            AIGene selectedGene;
            do {
                selectedGene = RandomUtility.randomPick(candidates);
            } while (selectedGene.getLifetime() > 0);
            AIGene resultantGene = (AIGene) selectedGene.mutate(true, false);
            scores.put(resultantGene, new GeneData(scores.get(selectedGene) == null ? 0 : scores.get(selectedGene).score, 0,
                    new HashSet<Gene>(Arrays.asList(selectedGene))));
            if (scores.get(selectedGene) == null){
                throw new AssertionError();
            }
            scores.get(selectedGene).connectedGenes.add(resultantGene);
            allGenes.add(resultantGene);
            mutated++;
        }*/
    }

    public Set<AI> generateWave(int wave, Player player, Set<AI> oldEnemies, Set<AIGene> allGenes) {
        Set<AI> enemies = new HashSet<>();

        Shape avoidArea = new Ellipse(player.getX(), player.getY(), 120, 120);

        if (allGenes.isEmpty()) {
            //initial state
            for (int i = 0; i < 4; ++i) {
                AIGene gene = AIGene.initialEnemyGene();
                enemies.add(AI.createEnemy(gene, avoidArea));
                allGenes.add(gene);
                scores.put(gene, new GeneData());
            }
        } else {
            if (!oldEnemies.isEmpty()) {
                this.updateGeneScores(oldEnemies, allGenes);
                this.createNewGenes(allGenes);
            }
            
            int enemyCount = RandomUtility.randBetween((int) Math.pow(wave, 0.5) + 3, (int) Math.pow(wave, 0.8) + 5);

            double waveScore = (wave + 3) * 50;

            double min = Double.MAX_VALUE;
            for (AIGene i : allGenes) {
                if (scores.get(i).score < min) {
                    min = scores.get(i).score;
                }
            }
            
            Set<AIGene> minGenes = new HashSet<>();
            for (AIGene i : allGenes) {
                if (scores.get(i).score < min * 1.1) {
                    minGenes.add(i);
                }
            }

            double currentScore = 0;
            do {
                AIGene candidate;
                if (currentScore > waveScore){
                    candidate = RandomUtility.randomPick(minGenes);
                } else {
                    candidate = RandomUtility.randomPick(allGenes);
                }
                currentScore += scores.get(candidate).score;
                enemies.add(AI.createEnemy(candidate, avoidArea));
            } while (enemies.size() < enemyCount);
            if (currentScore < waveScore){
                elitism = true;
            } else {
                elitism = false;
            }
            
        }

        return enemies;
    }
}
