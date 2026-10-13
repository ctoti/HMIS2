package com.pha.common.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.pha.common.error.ProblemDetailsAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class CommonWebAutoConfigurationTest {

    private final AutoConfigurations config = AutoConfigurations.of(CommonWebAutoConfiguration.class);

    @Test
    void registersFilterAndAdviceInServletApps() {
        new WebApplicationContextRunner()
                .withConfiguration(config)
                .run(context -> {
                    assertThat(context).hasSingleBean(FilterRegistrationBean.class);
                    assertThat(context).hasSingleBean(ProblemDetailsAdvice.class);
                });
    }

    @Test
    void staysOutOfNonWebApps() {
        new ApplicationContextRunner()
                .withConfiguration(config)
                .run(context -> assertThat(context).doesNotHaveBean(ProblemDetailsAdvice.class));
    }
}
