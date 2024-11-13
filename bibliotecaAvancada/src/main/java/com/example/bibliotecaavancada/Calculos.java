package com.example.bibliotecaavancada;

import android.graphics.Color;
import java.util.List;

//Aqui vai ser aonde eu coloco os calculos da biblioteca
public class Calculos {

    //Método auxiliar para calcular a distância entre dois carros
    public static double calcularDistancia(int x1, int y1, int x2, int y2){
        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
    }
    public static double normalizarAngulo(double angulo) {
        while (angulo > Math.PI) angulo -= 2 * Math.PI;
        while (angulo < -Math.PI) angulo += 2 * Math.PI;
        return angulo;
    }
    public static int[] calcularCentroMassa(List<int[]> sensoresCoords, List<Integer> sensoresPesos, int x, int y, int carSize, List<Integer> pixelsPista) {
        int somaX = 0, somaY = 0, validos = 0;

        for (int i = 0; i < sensoresCoords.size(); i++) {
            if (pixelsPista.get(i) != Color.BLACK) {
                somaX += sensoresCoords.get(i)[0] * sensoresPesos.get(i);
                somaY += sensoresCoords.get(i)[1] * sensoresPesos.get(i);
                validos += sensoresPesos.get(i);
            }
        }
        return validos > 0 ? new int[]{somaX / validos, somaY / validos} : new int[]{x + carSize / 2, y + carSize / 2};
    }
    public static int ajustarVelocidade(int velocidade, int velocidadeAlvo, int velocidadeMaxima) {
        // Ajusta a velocidade gradualmente em direção à velocidade alvo,
        // mas limita à velocidade máxima.

        if (velocidade < velocidadeAlvo) {
            return Math.min(velocidade + 1, velocidadeMaxima); // Limita a velocidadeMaxima
        } else if (velocidade > velocidadeAlvo) {
            return Math.max(velocidade - 1, 0); // Limita a velocidade mínima a 0
        }
        return velocidade;
    }


}
