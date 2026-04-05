package com.gamelog.gamelog.model;

import com.gamelog.gamelog.validation.genre.GenreValidationImpl;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "genre")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(GenreValidationImpl.class)
public class Genre extends MasterEntity{

    @NotBlank(message = "Name is required")
    private String name;
}
