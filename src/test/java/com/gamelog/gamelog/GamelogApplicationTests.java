package com.gamelog.gamelog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GamelogApplicationTests {

    @Test
    void shouldInstantiateApplicationClass() {
        assertThat(new GamelogApplication()).isNotNull();
    }
}
