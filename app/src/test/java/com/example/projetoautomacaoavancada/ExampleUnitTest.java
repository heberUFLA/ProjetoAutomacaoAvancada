package com.example.projetoautomacaoavancada;

import org.junit.Test;
import com.example.bibliotecaavancada.Calculos;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
    @org.junit.jupiter.api.Test
    public void testCalcularDistanciaEntreCarros() {

        int x1 = 0;
        int y1 = 0;
        int x2 = 3;
        int y2 = 4;

        // A distância entre (0,0) e (3,4) deve ser 5
        double distancia = Calculos.calcularDistancia(x1, y1, x2, y2);
        assertEquals(5, distancia);
    }
    @org.junit.jupiter.api.Test
    public void testAjustarVelocidade() {
        Car car = new Car("Carro 1", 0, 0, 0, null, 0, 0, 0, null, null);
        car.velocidadeMaxima = 10;

        // Teste quando a velocidade é menor que a velocidade alvo
        car.velocidade = 5;
        car.velocidadeAlvo = 8;
        car.ajustarVelocidade();
        assertEquals(6, car.velocidade); // Velocidade aumenta gradualmente

        // Teste quando a velocidade é maior que a velocidade alvo
        car.velocidade = 8;
        car.velocidadeAlvo = 5;
        car.ajustarVelocidade();
        assertEquals(7, car.velocidade); // Velocidade diminui gradualmente

        // Teste quando a velocidade é igual à velocidade alvo
        car.velocidade = 8;
        car.velocidadeAlvo = 8;
        car.ajustarVelocidade();
        assertEquals(8, car.velocidade); // Velocidade não muda

        // Teste quando a velocidade chega ao limite máximo
        car.velocidade = 9;
        car.velocidadeAlvo = 15;  // Velocidade alvo é maior que a máxima
        car.ajustarVelocidade();
        assertEquals(10, car.velocidade); // A velocidade deve ser limitada à velocidade máxima
    }
    @org.junit.jupiter.api.Test
    public void testNormalizarAngulo() {

        // Teste com um ângulo maior que PI
        double resultado = Calculos.normalizarAngulo(4 * Math.PI);
        assertEquals(-Math.PI, resultado, 1e-6);

        // Teste com um ângulo menor que -PI
        resultado = Calculos.normalizarAngulo(-4 * Math.PI);
        assertEquals(Math.PI, resultado, 1e-6);

        // Teste com um ângulo dentro do intervalo [-PI, PI]
        resultado = Calculos.normalizarAngulo(Math.PI / 2);
        assertEquals(Math.PI / 2, resultado, 1e-6);

        // Teste com um ângulo negativo dentro do intervalo
        resultado = Calculos.normalizarAngulo(-Math.PI / 2);
        assertEquals(-Math.PI / 2, resultado, 1e-6);
    }

}

