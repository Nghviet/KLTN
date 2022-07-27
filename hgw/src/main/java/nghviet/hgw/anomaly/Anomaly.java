package nghviet.hgw.anomaly;

import nghviet.hgw.http.HttpHandler;
import nghviet.hgw.utility.FileIO;
import nghviet.hgw.utility.LoggerHandler;
import nghviet.hgw.utility.MACAddress;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class Anomaly {
    final float INF = 1000000007;

    private Boolean modelLoaded = false;
    private static Anomaly instance = null;
    public static Anomaly getInstance() {
        if(instance == null) instance = new Anomaly();
        return instance;
    }

    ArrayList<StateRules> stateRules = new ArrayList<>();
    HashMap<String, Discrete> discreteValue = new HashMap<>();
    private class StateRules {
       HashMap<String, Boolean> antecedents = new HashMap<>(), consequents = new HashMap<>();
        float actecedent_support, consequent_support, support, confidence, lift, leverage, conviction;

        StateRules(String line) {
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
                if(antecedents.containsKey(state)) antCount++;
                if(consequents.containsKey(state)) consCount++;
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



    public void init() {
        if(modelLoaded) return;

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

        for(int i=1;i<state.size();i++) stateRules.add(new StateRules(state.get(i)));
        LoggerHandler.getInstance().info("State extract complete with " + stateRules.size());
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
        modelLoaded = true;
    }

    public ArrayList<String> anomalyDetection(ArrayList<String> inputStates) {

        if(stateRules.size() <= 1) return inputStates;
        Collections.sort(inputStates);
        int[][] values = new int[inputStates.size()][inputStates.size()];

        for(StateRules stateRules : this.stateRules) if(stateRules.isSubset(inputStates)) {
            for(String ant : stateRules.antecedents.keySet()) for(String con: stateRules.consequents.keySet()) {
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

    class State implements Comparable<State>{
        long timestamp;
        String state;
        int label = -2;
        String device;
        protected State(long timestamp, String ip, String type, String instanceCode, String value) {
            this.timestamp = timestamp;
            device = type + "/" + MACAddress.getInstance().getMACAddress(ip) + "/" + instanceCode;

            try {
                float convertedValue = Float.parseFloat(value);
                state = device + "_" + discreteValue.get(device).getLabel(convertedValue);
                label = discreteValue.get(device).getLabel(convertedValue);

            } catch (NumberFormatException e) {
                state = device + "_" + value;
                if(value.equals("ON")) label = -1;
            }
        }

        @Override
        public int compareTo(State o) {
            if(this.timestamp < o.timestamp) return 1;
            return 0;
        }
    }
    long start = -1000000007;
    ArrayList<State> states = new ArrayList<>();

    public void addState(long timestamp, String ip, String type, String instanceCode, String value) {
        states.add(new State(timestamp, ip, type, instanceCode, value));
        if(start < 0) start = timestamp;
    }

    public ArrayList<String> run() {
        if(!modelLoaded) return null;
        synchronized (states) {
            if(states.size() == 0) return null;


            ArrayList<String> result = new ArrayList<>();
            HashMap<String ,Boolean> a = new HashMap<>();

            while(states.size() > 0 && states.get(0).timestamp <= start + 2 * 60 * 1000) {
                    ArrayList<String> picked = new ArrayList<>();
                    HashMap<String, Integer> available = new HashMap<>();

                    while(states.size() > 0 && states.get(0).timestamp <= start + 2 * 60 * 1000) {

                        State state = states.get(0);
                        if(!available.containsKey(state.device)) available.put(state.device, state.label);
                        else available.put(state.device, Math.max(state.label, available.get(state.device)));
                        states.remove(0);
                    }

                    for(String device: available.keySet()) {
                        int label = available.get(device);
                        String l = "";
                        if(label < 0) l = "ON";
                        if(label >= 0) l = "" + label;
                        picked.add(device+"_"+l);
                    }

                    ArrayList<String> anomalyStates = anomalyDetection(picked);
                    for(String state : anomalyStates) {
                        System.out.println(state);
                        String device = state.split("_")[0];
                        if(!a.containsKey(device)) {
                            a.put(device, true);
                            result.add(device);
                        }
                    }

                    start += 2 * 60 * 1000;
            }

            return result;
        }
    }
}
