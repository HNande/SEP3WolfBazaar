package org.example.data.entities;

import jakarta.persistence.*;


@Entity
@Table(name = "category")
public class Category {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int categoryId;

  @Column(nullable = false, unique = true)
  private String name;

  protected Category() {
  }

  public Category(String name) {
    this.name = name;
  }

  public int getCategoryId() {
    return categoryId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}