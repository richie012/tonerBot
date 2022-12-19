package com.example.tonerBot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity(name = "WeeklyOrderTable")
public class WeeklyOrder {
    @Id
    private Long orderId;
    private String name;
    private String Date;
    private boolean isReady;
    private String comment;


}
