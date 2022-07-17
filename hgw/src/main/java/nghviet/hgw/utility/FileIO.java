package nghviet.hgw.utility;

import java.io.*;
import java.util.ArrayList;

public class FileIO {
    private static FileIO instance = null;

    public static FileIO getInstance() {
        if(instance == null) instance = new FileIO();
        return instance;
    }

    public void writeTo(String dir, ArrayList<String> data) throws Exception {
        File file = new File(dir);
        if(!file.exists()) file.createNewFile();
        FileWriter writer = new FileWriter(file);
        for(String line:data) writer.write(line + '\n');
        writer.flush();
        writer.close();
    }

    public ArrayList<String> readFrom(String dir)  throws Exception {

        File file = new File(dir);
        if(!file.exists()) throw new RuntimeException("File not exists");

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;
        ArrayList<String> data = new ArrayList<>();

        while((line = reader.readLine()) != null) {
            data.add(line);
        }

        reader.close();

        return data;
    }
}
