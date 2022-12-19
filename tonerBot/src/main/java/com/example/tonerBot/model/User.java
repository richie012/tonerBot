package com.example.tonerBot.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity(name = "usersTable")
public class User {
    @Id
    private Long chatId;
    private String firstName;
    private String lastName;
    private String registeredTime;

    @Override
    public String toString() {
        return "User{" +
                "chatId=" + chatId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", registeredTime='" + registeredTime + '\'' +
                '}';
    }
}
