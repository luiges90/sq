
package sq;

import java.util.Set;

/**
 *
 * @author Peter
 */
public interface WaveGeneratorInterface {
    
    public void loadData();
    
    public void saveData();

    public Set<AI> generateWave(int wave, Player player, Set<AI> oldEnemies, Set<AIGene> allGenes);
    
}
