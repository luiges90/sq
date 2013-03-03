package sq.waveGenerators;

import glabs.RandomUtility;
import java.util.*;
import org.newdawn.slick.geom.Ellipse;
import org.newdawn.slick.geom.Shape;
import sq.*;
/**
 * An unused implementation that had all idea I had. Including boss levels, stage-wide score for all genes, 
 * and gene elimination (of having near scores).
 * @author Peter
 */
/*public class FullImplementationGenerator implements WaveGeneratorInterface {

    public Set<AI> generateWave(int wave, Player player, Set<AI> oldEnemies, Set<AIGene> allGenes) {
        Set<AI> enemies = new HashSet<>();

        Shape avoidArea = new Ellipse(player.getX(), player.getY(), 120, 120);

        if (allGenes.isEmpty()) {
            //initial state
            for (int i = 0; i < 4; ++i) {
                AIGene gene = AIGene.initialEnemyGene();
                enemies.add(AI.createEnemy(gene, avoidArea));
                allGenes.add(gene);
            }
        } else {
            if (!oldEnemies.isEmpty()) {
                this.updateGeneScores(oldEnemies, allGenes);
                this.createNewGenes(allGenes);
            }

            int enemyCount = RandomUtility.randBetween((int) Math.pow(wave, 0.5) + 3, (int) Math.pow(wave, 0.8) + 5);

            int waveScore = (wave + 4) * 10000;
            float bossBonus = 1;
            if (wave % 5 == 0){
                bossBonus = wave / 20.0f + 1;
                waveScore *= bossBonus;
                enemyCount *= bossBonus;
            }

            float min = Float.MAX_VALUE;
            for (AIGene i : allGenes) {
                if (i.score < min) {
                    min = i.score;
                }
            }

            int[] targetScores = RandomUtility.getIntegerSumTo(waveScore, enemyCount);

            List<AIGene> enemyGeneList = new ArrayList<>(allGenes);

            for (int i = 0; i < enemyCount; ++i) {
                boolean foundCandidate = false;
                for (int k = 0;; k++) {
                    Collections.shuffle(enemyGeneList);
                    for (AIGene j : enemyGeneList) {
                        int normalized = Math.round(j.score * 10000 / min);
                        if (normalized <= targetScores[i] * (1 + k * 0.2) && normalized >= targetScores[i] * (1 - k * 0.1)) {
                            enemies.add(AI.createEnemy(j, avoidArea));
                            j.appearance++;
                            foundCandidate = true;
                            break;
                        }
                    }
                    if (foundCandidate) {
                        break;
                    }
                }
            }

        }

        return enemies;
    }

    public void updateGeneScores(Set<AI> oldEnemies, Set<AIGene> allGenes) {
        int totalKills = 0;
        int maxLongetivity = 0;
        float maxNearHitScore = 0;

        //compute total scores
        for (AI i : oldEnemies) {
            totalKills += i.getKills();
            if (i.getLongetivity() > maxLongetivity) {
                maxLongetivity = i.getLongetivity();
            }
            if (i.getNearHitScore() > maxNearHitScore) {
                maxNearHitScore = i.getNearHitScore();
            }
        }
        float stageScore = (float) ((totalKills * 10000 + maxLongetivity / 100 + maxNearHitScore) / Math.pow(oldEnemies.size(), 0.7));

        //compute score for individual genes
        Map<AIGene, Set<AI>> geneAndOwner = new HashMap<>();
        for (AI i : oldEnemies) {
            AIGene g;
            AI owner = i;
            if (i.getGene().getLifetime() < 0) {
                g = i.getGene();
                if (geneAndOwner.containsKey(g)) {
                    geneAndOwner.get(g).add(i);
                } else {
                    Set<AI> s = new HashSet<>();
                    s.add(i);
                    geneAndOwner.put(g, s);
                }
            } else {
                do {
                    owner = (AI) owner.getOwner();
                    g = owner.getGene();
                } while (g.getLifetime() > 0);
                if (geneAndOwner.containsKey(g)) {
                    geneAndOwner.get(g).add(i);
                } else {
                    Set<AI> s = new HashSet<>();
                    s.add(i);
                    geneAndOwner.put(g, s);
                }
            }
            g.score = (float) ((g.score * g.appearance + Math.pow(owner.getKills(), 2) * 1000 + 
                             Math.pow(owner.getLongetivity() , 2) / 100 + Math.pow(owner.getNearHitScore(), 2)) / (g.appearance + 1));
        }

        for (Map.Entry<AIGene, Set<AI>> i : geneAndOwner.entrySet()) {
            totalKills = 0;
            maxLongetivity = 0;
            maxNearHitScore = 0;
            for (AI j : i.getValue()) {
                totalKills += j.getKills();
                if (j.getLongetivity() > maxLongetivity) {
                    maxLongetivity = j.getLongetivity();
                }
                if (j.getNearHitScore() > maxNearHitScore) {
                    maxNearHitScore = j.getNearHitScore();
                }
            }

            i.getKey().score = i.getKey().score * 0.75f
                    + stageScore * 0.5f + totalKills * 10000 + maxLongetivity / 100 + maxNearHitScore;
        }

        //eliminate score-similar genes
        if (allGenes.size() > 20){
            for (Gene i : allGenes){
                for (Gene j : allGenes){
                    if (i == j) continue;
                    if (Math.abs(i.score - j.score) < i.nearestScore){
                        i.nearestScore = Math.abs(i.score - j.score);
                        j.nearestScore = Math.abs(i.score - j.score);
                    }
                }
            }
            List<AIGene> nearScores = new ArrayList<>(allGenes);
            Collections.sort(nearScores, new Comparator<AIGene>(){
                @Override
                public int compare(AIGene o1, AIGene o2) {
                    return o2.nearestScore > o1.nearestScore ? 1 : -1;
                }
            });
            Iterator<AIGene> it = allGenes.iterator();
            int nsi = 0;
            while (RandomUtility.randBetween(0, allGenes.size()) > 20){
                while (it.hasNext()){
                    AIGene i = it.next();
                    if (i.equals(nearScores.get(nsi))){
                        it.remove();
                        nsi++;
                    }
                }
            }
        }
    }

    public void createNewGenes(Set<AIGene> allGenes) {
        //mutate genes and create new ones
        if (allGenes.size() < 50) {
            for (int i = 0; i < 2; ++i) {
                AIGene selectedGene;
                do {
                    selectedGene = RandomUtility.randomPick(allGenes);
                } while (selectedGene.getLifetime() > 0);
                AIGene resultantGene = (AIGene) selectedGene.mutate(false);
                resultantGene.score = selectedGene.score;
                allGenes.add(resultantGene);
            }
        } else {
            while (RandomUtility.randBetween(0, allGenes.size()) < 10){
                AIGene selectedGene;
                do {
                    selectedGene = RandomUtility.randomPick(allGenes);
                } while (selectedGene.getLifetime() > 0);
                AIGene resultantGene = (AIGene) selectedGene.mutate(false);
                resultantGene.score = selectedGene.score;
                allGenes.add(resultantGene);
            }
        }

        //tweak more genes
        if (allGenes.size() < 50) {
            for (int i = 0; i < 5; ++i) {
                AIGene selectedGene;
                do {
                    selectedGene = RandomUtility.randomPick(allGenes);
                } while (selectedGene.getLifetime() > 0);
                AIGene resultantGene = (AIGene) selectedGene.mutate(true);
                resultantGene.score = selectedGene.score;
                allGenes.add(resultantGene);
            }
        } else{
            while (RandomUtility.randBetween(0, allGenes.size()) < 20){
                AIGene selectedGene;
                do {
                    selectedGene = RandomUtility.randomPick(allGenes);
                } while (selectedGene.getLifetime() > 0);
                AIGene resultantGene = (AIGene) selectedGene.mutate(true);
                resultantGene.score = selectedGene.score;
                allGenes.add(resultantGene);
            }
        }
    }
}
*/