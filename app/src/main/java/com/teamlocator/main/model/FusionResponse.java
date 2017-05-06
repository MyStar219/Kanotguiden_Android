package com.teamlocator.main.model;

import java.util.List;

/**
 * Created by kiril on 28.03.2017.
 */

public class FusionResponse {
    private String kind;
    private List<String> columns;
    private List<List<Object>> rows;

    public List<List<Object>> getRows() {
        return rows;
    }
}
