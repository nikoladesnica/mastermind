package com.nikoladesnica.mastermind.infra.generator;

import com.nikoladesnica.mastermind.domain.model.Code;
import com.nikoladesnica.mastermind.domain.ports.SecretCodeGenerator;
import com.nikoladesnica.mastermind.infra.config.GameProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class RandomOrgCodeGenerator implements SecretCodeGenerator {
    private final GameProperties props;
    private final HttpClient client;

    public RandomOrgCodeGenerator(GameProperties props) {
        this.props = props;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.randomOrg().timeoutMs()))
                .build();
    }

    @Override
    public Code generate() {
        try {
            String url = props.randomOrg().baseUrl()
                    + "?num=" + props.codeLength()
                    + "&min=" + props.minDigit()
                    + "&max=" + props.maxDigit()
                    + "&col=1&base=10&format=plain&rnd=new";

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(props.randomOrg().timeoutMs()))
                    .GET()
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new IllegalStateException("Random.org bad status: " + res.statusCode());
            String body = res.body().trim();
            List<Integer> digits = new ArrayList<>();
            for (String line : body.split("\\R")) {
                if (!line.isBlank()) digits.add(Integer.parseInt(line.trim()));
            }
            // if duplicates not allowed, adjust on top (rarely needed; spec allows duplicates by default)
            if (!props.allowDuplicates() && digits.stream().distinct().count() != digits.size()) {
                return new LocalCodeGenerator(props).generate();
            }
            return new Code(digits, props.codeLength(), props.minDigit(), props.maxDigit(), props.allowDuplicates());
        } catch (Exception e) {
            // availability first: fallback locally
            return new LocalCodeGenerator(props).generate();
        }
    }
}
