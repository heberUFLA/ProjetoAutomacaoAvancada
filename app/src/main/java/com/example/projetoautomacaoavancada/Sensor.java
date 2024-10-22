package com.example.projetoautomacaoavancada;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;


public class Sensor {
    private int distSensor;
    private String name;
    private double anguloSensor; // Ângulo do sensor RELATIVO ao carro
    private Paint sensorPaint;
    private Paint areaSensorPaint;
    private int[] sensorCoords;
    private boolean limite = true;
    private boolean pixelBlack=true;
    private int distSensorDinam=0;


    public Sensor(String name, int x, int y, double anguloCarro, double anguloSensor, int distSensor, int carSize) {
        this.name = name;
        this.distSensor = distSensor;
        this.anguloSensor = anguloSensor;
        inicializarCores();

        // Inicia as coordenadas do sensor com base nas coordenadas do carro, do ângulo do carro e do ângulo do sensor
        int centroX = x + carSize / 2;
        int centroY = y + carSize / 2;
        int sensorX = (int) (centroX + Math.cos(anguloCarro + anguloSensor) * distSensor);
        int sensorY = (int) (centroY + Math.sin(anguloCarro + anguloSensor) * distSensor);
        sensorCoords = new int[]{sensorX, sensorY};
    }

    public int[] getSensorCoords(int _x, int _y, double anguloCarro, int carSize, Bitmap tempBitmap) {
        int centroX = _x + carSize / 2;
        int centroY = _y + carSize / 2;
        double anguloAbsoluto = anguloCarro + this.anguloSensor;

        int sensorX = (int) (centroX + Math.cos(anguloAbsoluto) * distSensor);
        int sensorY = (int) (centroY + Math.sin(anguloAbsoluto) * distSensor);
        sensorCoords = new int[]{sensorX, sensorY};

        return atualizaSensorCoords(new int[]{centroX, centroY}, anguloAbsoluto, tempBitmap);
    }

    public int[] atualizaSensorCoords(int[] coordCentroCarro, double anguloAbsoluto, Bitmap tempBitmap) {

        distSensorDinam=distSensor;
        limite = true;
        int pixel = tempBitmap.getPixel(sensorCoords[0], sensorCoords[1]);
        if (pixel != Color.BLACK) {
            Log.e("COR", name+ ": ENXERGO BRANCO");
            pixelBlack=false;
        }else{
            Log.e("COR", name + ": ENXERGO PRETO");
            pixelBlack=true;
        }

        while (limite == true && pixelBlack == true) { //Enquanto não tiver chegado ao limite do carr e a cor do pixel continuar sendo preta

            if (distSensorDinam > 15 ) {
                pixel = tempBitmap.getPixel(sensorCoords[0], sensorCoords[1]);
                if (pixel != Color.BLACK) {
                    pixelBlack = false;
                }else {
                    distSensorDinam--; // Diminui a distância para o próximo ponto
                    // Calcula as novas coordenadas do sensor com base no ângulo e na distância reduzida
                    sensorCoords[0] = (int) (coordCentroCarro[0] + Math.cos(anguloAbsoluto) * distSensorDinam);
                    sensorCoords[1] = (int) (coordCentroCarro[1] + Math.sin(anguloAbsoluto) * distSensorDinam);
                }
            } else {
                limite = false;
            }
        }
        return sensorCoords;
    }

    private void inicializarCores() {
        // Define a cor da linha do sensor
        sensorPaint = new Paint();
        sensorPaint.setColor(Color.RED);
        sensorPaint.setStrokeWidth(2f);

        // Define a cor da área do sensor
        areaSensorPaint = new Paint();
        areaSensorPaint.setColor(Color.argb(100, 0, 0, 255)); // Azul translúcido
        areaSensorPaint.setStyle(Paint.Style.FILL);
    }

    public double getAnguloSensor() {
        return anguloSensor;
    }

    public void setAnguloSensor(double anguloSensor) {
        this.anguloSensor = anguloSensor;
    }

    public void desenharSensor(Canvas canvas, int x, int y, int carSize) {

        // Ajusta a distância e coordenadas do sensore
        int centroX = x + carSize / 2;
        int centroY = y + carSize / 2;

        // Desenha a linha do sensor
        canvas.drawLine(centroX, centroY, sensorCoords[0], sensorCoords[1], sensorPaint);
    }

    public int getDistSensor() {
        return distSensor;
    }
    public int getDistSensorDinam() {
        return distSensorDinam;
    }
    public String getName() {
        return name;
    }

    public boolean verificarSensorLivre(Bitmap pistaBitmap, int xCarro, int yCarro, double anguloCarro, int carSize) {
        //Calcula a coordenada do sensor
        int[] coordSensor = getSensorCoords(xCarro, yCarro, anguloCarro, carSize, pistaBitmap);
        //Verifica se está dentro dos limites da pista e retorna se a cor do pixel é preta
        return coordSensor[0] >= 0 && coordSensor[0] < pistaBitmap.getWidth() &&
                coordSensor[1] >= 0 && coordSensor[1] < pistaBitmap.getHeight() &&
                pistaBitmap.getPixel(coordSensor[0], coordSensor[1]) == Color.BLACK;

    }
}
