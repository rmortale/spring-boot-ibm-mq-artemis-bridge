package com.example.springbootibmmq.configuration;

import lombok.Data;

@Data
public class QueueMapping {

    private String sourceQueue;
    private String targetQueue;

}
