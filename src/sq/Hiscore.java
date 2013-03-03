package sq;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Peter
 */
public class Hiscore {
    
    private static class HiscoreEntry{
        public int wave, kill;
        public HiscoreEntry(int w, int k){
            wave = w;
            kill = k;
        }
    }
    
    private List<HiscoreEntry> entry = new ArrayList<>();
    
    public Hiscore(){
        try (BufferedReader br = new BufferedReader(new FileReader("hiscore.txt"))){
            br.readLine(); //skip first line
            for (int i = 0; i < 10; ++i){
                String s = br.readLine();
                if (s == null){
                    entry.add(new HiscoreEntry(0, 0));
                } else {
                    String[] data = s.split("\\s");
                    entry.add(new HiscoreEntry(Integer.parseInt(data[1]), Integer.parseInt(data[2])));
                }
            }
        } catch (FileNotFoundException ex){
            for (int i = 0; i < 10; ++i){
                entry.add(new HiscoreEntry(0, 0));
            }
        } catch (IOException|NumberFormatException ex){
            //ignore.
        }
    }
    
    public Hiscore addNewEntry(int w, int k){
        for (int i = 0; i < 10; ++i){
            if (w > entry.get(i).wave || (w == entry.get(i).wave && k >= entry.get(i).kill)){
                for (int j = 9; j > i; --j){
                    entry.set(j, entry.get(j - 1));
                }
                entry.set(i, new HiscoreEntry(w, k));
                break;
            }
        }
        return this;
    }
    
    public Hiscore save(){
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("hiscore.txt"))){
            bw.write("Rank\tWave\tKills");
            bw.newLine();
            for (int i = 0; i < 10; ++i){
                bw.write((i + 1) + "\t" + entry.get(i).wave + "\t" + entry.get(i).kill);
                bw.newLine();
            }
        } catch (IOException ex){
            //ignore
        }
        return this;
    }
    
}
