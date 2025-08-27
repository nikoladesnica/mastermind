package com.nikoladesnica.mastermind.domain.ports;

import com.nikoladesnica.mastermind.domain.model.Code;

public interface SecretCodeGenerator {
    Code generate();
}
