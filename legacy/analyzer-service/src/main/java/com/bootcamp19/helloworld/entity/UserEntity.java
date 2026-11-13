package com.bootcamp19.helloworld.entity;

import jakarta.persistence.*;
import lombok.Data;

@Table(name = "mst_user")
@Entity
@Data
public class UserEntity {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;

   @Column(nullable = false, unique = true, length = 100)
    private String username;

   @Column(nullable = false)
    private String password;
}
