package com.nikoladesnica.mastermind.infra.generator;

import com.nikoladesnica.mastermind.domain.model.Code;
import com.nikoladesnica.mastermind.domain.ports.SecretCodeGenerator;
import com.nikoladesnica.mastermind.infra.config.GameProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LocalCodeGenerator implements SecretCodeGenerator {
    private final GameProperties props;

    public LocalCodeGenerator(GameProperties props) {
        this.props = props;
    }

    @Override
    public Code generate() {
        List<Integer> digits = new ArrayList<>(props.codeLength());
        while (digits.size() < props.codeLength()) {
            int d = ThreadLocalRandom.current().nextInt(props.minDigit(), props.maxDigit() + 1);
            if (props.allowDuplicates() || !digits.contains(d)) {
                digits.add(d);
            }
        }
        return new Code(digits, props.codeLength(), props.minDigit(), props.maxDigit(), props.allowDuplicates());
    }
}
