package net.nud.basemaps;

public interface OnTaskCompleted {

        void onTaskCompleted(String args);

        void onTaskCompleted(Boolean connected, Boolean updateWater, Boolean updateSewer, Boolean updateOrtho);
}
