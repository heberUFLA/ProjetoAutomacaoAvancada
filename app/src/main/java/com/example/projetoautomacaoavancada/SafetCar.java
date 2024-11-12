package com.example.projetoautomacaoavancada;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

public class SafetCar extends Car {
    private static final int VELOCIDADE_CONSTANTE = 1; // Velocidade constante mais alta
    private static final int COR_SAFE_CAR = Color.BLACK;// Cor predefinida para SafeCar
    private final int velocidadeConstante = 5;

    public SafetCar(String nome, int x, int y, int tamanho, Bitmap bitmap, double angulo, int velocidade, int cor, Bitmap pista, MainActivity mainActivity) {
        super(nome, x, y, tamanho, bitmap, angulo, velocidade, COR_SAFE_CAR, pista, mainActivity);
        // Garantir que a velocidade do SafetCar seja a constante
        super.setVelocidade(velocidadeConstante);
    }

    @Override
    public void run() {
        // Aqui você pode modificar o comportamento da thread do SafeCar para garantir a prioridade
        // quando ele tentar passar pela área restrita da pista.

        // Esse método precisa de lógica de prioridade, por exemplo:
        try {
            // Certifique-se de que este carro tenha prioridade em relação aos outros carros
            // Ao passar pela área com restrição de espaço, um mecanismo de semáforo pode ser usado
            setVelocidade(VELOCIDADE_CONSTANTE);
            // Executa a movimentação normal do carro
            super.run();
        } catch (Exception e) {
            Log.e("SafeCar", "Erro durante o movimento do carro seguro.", e);
        }
    }
    // Método para garantir que a velocidade não seja modificada
    @Override
    public void setVelocidade(int novaVelocidade) {
        // Não permite alterar a velocidade do SafetCar
        if (this.getVelocidade() != velocidadeConstante) {
            super.setVelocidade(novaVelocidade);
        }
    }


}