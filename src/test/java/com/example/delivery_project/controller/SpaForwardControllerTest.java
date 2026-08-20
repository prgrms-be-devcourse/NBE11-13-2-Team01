package com.example.delivery_project.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpaForwardControllerTest {

    private final SpaForwardController controller =
            new SpaForwardController();

    @Test
    void React_화면_경로를_index_html로_포워딩한다() {
        assertThat(controller.forwardToReact())
                .isEqualTo("forward:/index.html");
    }
}
