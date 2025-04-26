package ru.savin.notificationhub.config;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@Slf4j
@RequiredArgsConstructor
public class GreenMailConfig {

        private GreenMail greenMail;

        // Запуск GreenMail для локального использования
        @PostConstruct
        public void startGreenMail() {
                log.info("Starting GreenMail server...");
                greenMail = new GreenMail(new ServerSetup(3025, "localhost", "smtp"));
                greenMail.start();
                greenMail.setUser("login", "1234");
                log.info("GreenMail server started on localhost:3025");
        }

        // Остановка GreenMail при завершении
        @PreDestroy
        public void stopGreenMail() {
                if (greenMail != null) {
                        log.info("Stopping GreenMail server...");
                        greenMail.stop();
                        log.info("GreenMail server stopped.");
                }
        }

        @Bean
        public GreenMail greenMail() {
                return greenMail;
        }

}
