package nghviet.hgw.anomaly;

import nghviet.hgw.http.HttpHandler;
import nghviet.hgw.utility.FileIO;
import nghviet.hgw.utility.LoggerHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Anomaly {
    final float INF = 1000000007;
    private static Anomaly instance = null;
    public static Anomaly getInstance() {
        if(instance == null) instance = new Anomaly();
        return instance;
    }

    ArrayList<State> states = new ArrayList<>();
    HashMap<String, Discrete> discreteValue = new HashMap<>();
    private class State {
       HashMap<String, Boolean> antecedents = new HashMap<>(), consequents = new HashMap<>();
        float actecedent_support, consequent_support, support, confidence, lift, leverage, conviction;

        State(String line) {
            String[] cell = line.split(",");
            String[] a = cell[0].split(";"), c = cell[1].split(";");
            for(int i=0;i<a.length;i++) antecedents.put(a[i],true);
            for(int i=0;i<c.length;i++) consequents.put(c[i], true);

            actecedent_support = Float.parseFloat(cell[2]);
            consequent_support = Float.parseFloat(cell[3]);
            support = Float.parseFloat(cell[4]);
            confidence = Float.parseFloat(cell[5]);
            lift = Float.parseFloat(cell[6]);
            leverage = Float.parseFloat(cell[7]);
            try {
                conviction = Float.parseFloat(cell[8]);
            } catch(Exception e) {
                conviction = INF;
            }
        }

        public boolean isSubset(ArrayList<String> inputStates) {
            int antCount = 0, consCount = 0;
            for(String state: inputStates) {
                if(antecedents.containsKey(state) && antecedents.get(state)) antCount++;
                if(consequents.containsKey(state) && consequents.get(state)) consCount++;
            }
            if(antCount == antecedents.size() && consCount == consequents.size()) return true;
            return false;
        }
    }

    class Discrete {
        class Label {
            public int label;
            public float maxValue = - INF;
            public boolean available = false;
            Label(int label, float maxValue, boolean available) {
                this.label = label;
                this.maxValue = maxValue;
                this.available = available;
            }
        }

        ArrayList<Label> labels = new ArrayList<>();
        Discrete(float label_1_max, float label_2_max, float label_3_max, float label_4_max) {
            if(label_1_max == -INF) labels.add(new Label(1, -INF, false)); else labels.add(new Label(1, label_1_max, true));
            if(label_2_max == -INF) labels.add(new Label(2, -INF, false)); else labels.add(new Label(2, label_2_max, true));
            if(label_3_max == -INF) labels.add(new Label(3, -INF, false)); else labels.add(new Label(3, label_3_max, true));
            if(label_4_max == -INF) labels.add(new Label(4, -INF, false)); else labels.add(new Label(4, label_4_max, true));
            labels.add(new Label(5, INF, true));
        }

        int getLabel(float value) {
            for(int i=0;i<4;i++) {
                Label label = labels.get(i);
                if(!label.available) continue;
                if(value <= label.maxValue) return label.label;
            }
            return 5;
        }
    }

    HashMap<String, Boolean> pairAvailable = new HashMap<>();

    private Anomaly() {
        ArrayList<String> discrete, state, device;
        File file = new File("model/discrete.csv");
        if(!file.exists()) {
            discrete = HttpHandler.getInstance().request("GET", "http://112.137.129.202:8080/API/discrete",null, true);
            state = HttpHandler.getInstance().request("GET", "http://112.137.129.202:8080/API/state_pattern",null, true);
            device = HttpHandler.getInstance().request("GET", "http://112.137.129.202:8080/API/device_pattern",null, true);
            try {
                FileIO.getInstance().writeTo("model/discrete.csv", discrete);
                FileIO.getInstance().writeTo("model/state_pattern.csv", state);
                FileIO.getInstance().writeTo("model/device_pattern.csv", device);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            try {
                discrete = FileIO.getInstance().readFrom("model/discrete.csv");
                state = FileIO.getInstance().readFrom("model/state_pattern.csv");
                device = FileIO.getInstance().readFrom("model/device_pattern.csv");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        for(int i=1;i<state.size();i++) states.add(new State(state.get(i)));
        LoggerHandler.getInstance().info("State extract complete with : " + states.size() + " and " + pairAvailable.size() + " pair created");
        for(int i=1; i < discrete.size();i++) {
            String[] cell = discrete.get(i).split(",");
            float max_value_1 = -INF, max_value_2 = -INF, max_value_3 = -INF, max_value_4 = -INF;
            if(cell[1].length() > 0) max_value_1 = Float.parseFloat(cell[1]);
            if(cell[2].length() > 0) max_value_2 = Float.parseFloat(cell[2]);
            if(cell[3].length() > 0) max_value_3 = Float.parseFloat(cell[3]);
            if(cell[4].length() > 0) max_value_4 = Float.parseFloat(cell[4]);
            discreteValue.put(cell[0], new Discrete(max_value_1, max_value_2, max_value_3, max_value_4));
        }
        LoggerHandler.getInstance().info("Discrete value extract complete with : " + discreteValue.size());
    }

    public ArrayList<String> anomalyDetection(ArrayList<String> inputStates) {

        if(states.size() <= 1) return inputStates;

        Collections.sort(inputStates);

        int[][] values = new int[inputStates.size()][inputStates.size()];

        for(State state : states) if(state.isSubset(inputStates)) {
            for(String ant : state.antecedents.keySet()) for(String con: state.consequents.keySet()) {
                values[inputStates.indexOf(ant)][inputStates.indexOf(con)]++;
            }
        }

        ArrayList<String> candidates = new ArrayList<>();

        for(int i=0;i<inputStates.size();i++) {
            int row = 0;
            int col = 0;
            for(int j=0;j<inputStates.size();j++) {
                row += values[i][j];
                col += values[j][i];
            }

            if(row == 0 && col == 0) {
                candidates.add(inputStates.get(i));
            }
        }

        return candidates;
    }

    public String getState(String device, float value) {
        if(!discreteValue.containsKey(device)) return null;
        return "" + device + "_" + discreteValue.get(device).getLabel(value);
    }
}
