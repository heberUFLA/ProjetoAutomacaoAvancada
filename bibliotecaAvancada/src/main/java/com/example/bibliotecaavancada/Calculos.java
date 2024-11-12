package com.example.bibliotecaavancada;


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

}
