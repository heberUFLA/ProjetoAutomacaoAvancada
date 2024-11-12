package com.example.bibliotecaavancada;

import android.graphics.Bitmap;

public class EstadosCar {

    private boolean isSafetyCar;
    private String name;
    private int x;
    private int y;
    private int velocidade;
    private double angulo;
    private Bitmap carroBitmap;
    private int carSize;
    private int corOriginalCarro;
    private int voltasCompletadas;
    private int distanciaPercorrida;
    private int penalidade;

    public EstadosCar() {}

    public EstadosCar(String name, int x, int y, int carSize,
                      Bitmap carroBitmap, double angulo, int velocidade,int IDcorCarro,
                      int voltasCompletadas, int distanciaPercorrida, int penalidade, boolean isSafetyCar) {

        this.name = name;
        this.x = x;
        this.y = y;
        this.carSize = carSize;
        this.carroBitmap = carroBitmap;
        this.angulo = angulo;
        this.velocidade=velocidade;
        this.corOriginalCarro = IDcorCarro;
        this.voltasCompletadas = voltasCompletadas;
        this.distanciaPercorrida = distanciaPercorrida;
        this.penalidade = penalidade;
        this.isSafetyCar = isSafetyCar;

    }

    public String getNome(){
        return this.name;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getVelocidade() {
        return this.velocidade;
    }

    public double getAngulo() {
        return this.angulo;
    }

    public int getCarSize() {
        return this.carSize;
    }
    public Bitmap getCarroBitmap(){ return carroBitmap;}
    public int getCorOriginalCarro() {
        return this.corOriginalCarro;
    }
    public int getVoltasCompletadas() {
        return this.voltasCompletadas;
    }
    public int getDistanciaPercorrida() {
        return this.distanciaPercorrida;
    }
    public int getPenalidade() {
        return this.penalidade;
    }
    public boolean isSafetyCar() {
        return isSafetyCar;
    }

}
