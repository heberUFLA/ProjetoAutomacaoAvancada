package com.example.projetoautomacaoavancada;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import java.util.concurrent.Semaphore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Car extends Thread{
    private String name;
    private int x;
    private int y;
    private double angulo;
    private Bitmap carroBitmap;
    private int carSize;
    protected ArrayList<Sensor> sensores = new ArrayList<>();
    private Sensor sensorCentro, sensorEsquerda, sensorDireita, sensorEsquerdaDist, sensorDireitaDist;
    private int velocidade;
    private int voltasCompletadas=0;
    private int altura,largura;// Controle para evitar contagens repetidas
    private boolean cruzouLinha = false;// Controle de estado para a volta
    private Paint corCarro;
    private int xAnterior, yAnterior = 0;
    private int distanciaPercorrida = 0;
    private int penalidade = 0;
    private int velocidadeAlvo; // Velocidade que o carro tenta alcançar
    private int velocidadeMaxima; // Defina a velocidade máxima aqui
    private Bitmap pista;
    private boolean isRunning = true;  // Variável de controle para a thread
    private MainActivity mainActivity; // Referência à MainActivity
    // Cria um semáforo com 1 permissão para acesso exclusivo
    private final Semaphore semaforo = new Semaphore(1);
    private static final Semaphore areaCriticaSemaphore = new Semaphore(1); // Limite de um carro por vez
    // Definindo os limites da área crítica (exemplo)
    private static final int CRITICAL_AREA_START_X = 900;  // Posição X inicial
    private static final int CRITICAL_AREA_END_X = 1300;    // Posição X final
    private static final int CRITICAL_AREA_START_Y = 1700;   // Posição Y inicial
    private static final int CRITICAL_AREA_END_Y = 1900;
    private boolean naAreaCritica = false;// Posição Y final



    public Car(String name, int x, int y, int carSize,Bitmap carroBitmap, double angulo, int velocidade,Paint corCarro, Bitmap pista, MainActivity main) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.angulo = angulo;
        this.carroBitmap = carroBitmap;
        this.pista = pista;
        this.carSize = carSize;
        this.corCarro = corCarro;
        altura=carroBitmap.getHeight();
        largura=carroBitmap.getWidth();
        this.velocidade=velocidade;
        this.velocidadeAlvo = velocidade; // Inicializa a velocidade alvo com a velocidade inicial
        this.velocidadeMaxima = velocidade; // Defina a velocidade máxima aqui
        this.mainActivity = main;
        criarSensores();
    }
    @Override
    public void run() {
        while (true) {
            try {
                // Verifica se o carro deve entrar na área crítica e obtém permissão
                if (!naAreaCritica && deveEntrarNaAreaCritica()) {
                    areaCriticaSemaphore.acquire();
                    naAreaCritica = true; // Indica que o carro está na área crítica
                    Log.d("Car", "Entrou na área crítica.");
                }

                // Atualiza a posição do carro
                moverCarro();

                // Verifica colisões após a atualização de posição
                resolverColisao();

                // Verifica se o carro saiu da área crítica para liberar o semáforo
                if (naAreaCritica && saiuDaAreaCritica()) {
                    areaCriticaSemaphore.release();
                    naAreaCritica = false; // Indica que o carro saiu da área crítica
                    Log.d("Car", "Saiu da área crítica.");
                }

                // Pausa para simular a atualização da posição em intervalos
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break; // Encerra a thread em caso de interrupção
            }
        }
    }

    public void moverCarro() {
        //Calcula o centro de massa da área mapeada a frente do carro
        int[] centroMassa = calcularCentroMassa();
        //Log.e("COORDS CENTROIDE", "COORDS CENTROIDE: " + centroMassa[0] + " " + centroMassa[1] );

        //desenharCentroMassa(canvas, centroMassa[0], centroMassa[1]);

        //Faz a rotação do carro buscando virar a frente do carro para o centro de massa
        rotacionarCarroParaCentroMassa(centroMassa);
        //Armazena a coordenada atual do carro
        xAnterior = x;
        yAnterior = y;

        ajustarVelocidade();// Passa a velocidade alvo para

        //Atualiza a coordenada do carro
        x += Math.cos(angulo) * velocidade;
        y += Math.sin(angulo) * velocidade;
    }

    private int[] calcularCentroMassa() {

        int somaX = 0, somaY = 0, validos = 0;
        //acessa a coordenada do sensor e verifica se a cor do pixel não é preta
        for (Sensor sensor : sensores) {
            int[] coordSensor =sensor.getSensorCoords(x, y, angulo, carSize, pista);
            int peso=sensor.getDistSensorDinam();
            int pixel = pista.getPixel(coordSensor[0], coordSensor[1]);
            // Log.e("COORDS SENSOR", "COORDS SENSOR: " + coordSensor[0] + " " + coordSensor[1] + " " + pixel);
            if (pixel != Color.BLACK) {  // Sensor detecta pista válida
                somaX += (coordSensor[0]*peso);
                somaY += (coordSensor[1]*peso);
                validos+=peso;
            }
        }

        if (validos > 0) {
            return new int[]{somaX / validos, somaY / validos};
        } else {
            // Se nenhum sensor detectar, mantém o carro na mesma direção
            return new int[]{x + carSize / 2, y + carSize / 2};
        }
    }

    private void rotacionarCarroParaCentroMassa(int[] centroMassa) {
        int centroCarroX = x + carSize / 2;
        int centroCarroY = y + carSize / 2;

        // Calcula o ângulo alvo com relação ao centro de massa
        double anguloAlvo = Math.atan2(centroMassa[1] - centroCarroY, centroMassa[0] - centroCarroX);

        // Normaliza os ângulos
        angulo = normalizarAngulo(angulo);
        anguloAlvo = normalizarAngulo(anguloAlvo);


        // Calcula a menor diferença angular no sentido anti-horário
        double diferenca = anguloAlvo - angulo;
        diferenca = normalizarAngulo(diferenca); // Garante que está entre -PI e PI

        // Verifica a menor rotação (anti-horário ou horário)
        if (diferenca > Math.PI) {
            diferenca -= 2 * Math.PI;
        } else if (diferenca < -Math.PI) {
            diferenca += 2 * Math.PI;
        }

        // Aplica uma rotação suave e limitada
        angulo += Math.signum(diferenca) * Math.min(Math.abs(diferenca), 10);

    }

    private void resolverColisao() {
        int distanciaMinima = carSize + 2; // Distância mínima segura entre os carros
        double distanciaDesvio = carSize * 2; // Distância para começar a desviar
        int ajuste = 3; // Valor de ajuste de posição em pixels

        List<Car> carros = mainActivity.getCarros();

        if (mainActivity == null || carros == null) {
            Log.e("Car", "MainActivity ou lista de carros está nula");
            return;
        }

        // Copia a lista de carros para evitar ConcurrentModificationException
        List<Car> carrosCopia;
        try {
            semaforo.acquire();
            carrosCopia = new ArrayList<>(carros); // Cria uma cópia local da lista de carros
        } catch (InterruptedException e) {
            Log.e("Car", "Erro ao adquirir semáforo: " + e.getMessage());
            return;
        } finally {
            semaforo.release();
        }

        for (Car outroCarro : carrosCopia) {
            if (outroCarro != this) {
                double distancia = calcularDistancia(this, outroCarro);
                double dx = outroCarro.getX() - this.getX();
                double dy = outroCarro.getY() - this.getY();

                if (distancia < distanciaMinima) {
                    this.penalidade++;
                    outroCarro.penalidade++;

                    double distanciaEntreCarros = Math.sqrt(dx * dx + dy * dy);
                    double sobreposicao = distanciaMinima - distanciaEntreCarros;

                    if (sobreposicao > 0) {
                        dx /= distanciaEntreCarros;
                        dy /= distanciaEntreCarros;

                        double deslocamentoX = dx * (sobreposicao / 2.0 + ajuste);
                        double deslocamentoY = dy * (sobreposicao / 2.0 + ajuste);

                        this.setX(this.getX() - (int) deslocamentoX);
                        this.setY(this.getY() - (int) deslocamentoY);
                        outroCarro.setX(outroCarro.getX() + (int) deslocamentoX);
                        outroCarro.setY(outroCarro.getY() + (int) deslocamentoY);

                        if (this.velocidade > outroCarro.velocidade) {
                            this.velocidade = outroCarro.velocidade;
                        }
                    }
                } else if (distancia < distanciaDesvio) {
                    double deslocamentoX = dx * ajuste / distancia;
                    double deslocamentoY = dy * ajuste / distancia;

                    this.setX(this.getX() - (int) deslocamentoX);
                    this.setY(this.getY() - (int) deslocamentoY);
                    outroCarro.setX(outroCarro.getX() + (int) deslocamentoX);
                    outroCarro.setY(outroCarro.getY() + (int) deslocamentoY);

                    boolean esquerdaLivre1 = this.sensores.get(1).verificarSensorLivre(pista, this.x, this.y, this.angulo, carSize);
                    boolean direitaLivre1 = this.sensores.get(5).verificarSensorLivre(pista, this.x, this.y, this.angulo, carSize);
                    boolean esquerdaLivre2 = this.sensores.get(2).verificarSensorLivre(pista, this.x, this.y, this.angulo, carSize);
                    boolean direitaLivre2 = this.sensores.get(5).verificarSensorLivre(pista, this.x, this.y, this.angulo, carSize);

                    if (!esquerdaLivre1 || !direitaLivre1 || !esquerdaLivre2 && !direitaLivre2) {
                        this.velocidadeAlvo = Math.min(outroCarro.velocidade, this.velocidadeMaxima);
                    }
                } else if (carroSaiuDaPista(this, pista)) {
                    this.penalidade++;
                } else {
                    this.velocidadeAlvo = this.velocidadeMaxima;
                }
            }
        }
    }

    // Método que verifica se o carro deve entrar na área crítica
    private boolean deveEntrarNaAreaCritica() {
        return this.getX() >= CRITICAL_AREA_START_X && this.getX() <= CRITICAL_AREA_END_X &&
                this.getY() >= CRITICAL_AREA_START_Y && this.getY() <= CRITICAL_AREA_END_Y;
    }

    // Método que libera o semáforo ao sair da área crítica
    private boolean saiuDaAreaCritica() {
        // Verifica se o carro está fora dos limites da área crítica
        return this.x < CRITICAL_AREA_START_X || this.x > CRITICAL_AREA_END_X ||
                this.y < CRITICAL_AREA_START_Y || this.y > CRITICAL_AREA_END_Y;
    }



    private boolean carroSaiuDaPista(Car carro, Bitmap pistaBitmap) {
        int x = carro.x + carro.carroBitmap.getWidth() / 2;
        int y = carro.y + carro.carroBitmap.getHeight() / 2;
        // Verifica se o centro do carro está fora dos limites da imagem
        if (x < 0 || x >= pistaBitmap.getWidth() || y < 0 || y >= pistaBitmap.getHeight()) {
            return true; // Saiu da pista
        }
        // Verifica se o centro do carro está em um pixel preto (fora da pista)
        return pistaBitmap.getPixel(x, y) == Color.BLACK;
    }

    //Método auxiliar para calcular a distância entre dois carros
    private double calcularDistancia(Car carro1, Car carro2) {
        return Math.sqrt(Math.pow(carro1.x - carro2.x, 2) + Math.pow(carro1.y - carro2.y, 2));
    }

    //Método para reposicionar os carros após uma colisão
    private void reposicionarCarros(Car carro1, Car carro2, int distanciaMinima) {
        // Calcula o vetor entre os carros
        double dx = carro2.x - carro1.x;
        double dy = carro2.y - carro1.y;
        double angulo = Math.atan2(dy, dx);

        // Calcula as novas posições para evitar a sobreposição
        double novaPosicaoX1 = carro2.x - distanciaMinima * Math.cos(angulo);
        double novaPosicaoY1 = carro2.y - distanciaMinima * Math.sin(angulo);

        carro1.x = (int) novaPosicaoX1;
        carro1.y = (int) novaPosicaoY1;
    }

    private void ajustarVelocidade() {
        // Ajusta a velocidade gradualmente em direção à velocidade alvo,
        // mas limita à velocidade máxima.

        if (velocidade < velocidadeAlvo) {
            velocidade = Math.min(velocidade + 1, velocidadeMaxima); // Limita a velocidadeMaxima
        } else if (velocidade > velocidadeAlvo) {
            velocidade = Math.max(velocidade - 1, 0); // Limita a velocidade mínima a 0
        }
    }

    private boolean colisao(Car carro1, Car carro2) {
        return carro1.getX() < carro2.getX() + carro2.getLargura()-10 &&
                carro1.getX() + carro1.getLargura()-10 > carro2.getX() &&
                carro1.getY() < carro2.getY() + carro2.getAltura()-10 &&
                carro1.getY() + carro1.getAltura()-10 > carro2.getY();
    }


    public void calcularDistancia() {
        //Calcula a distancia percorrida com base no princípio da distancia entre dois pontos
        distanciaPercorrida += (int)(Math.sqrt(Math.pow(x - xAnterior, 2) + Math.pow(y - yAnterior, 2)));
    }

    private void criarSensores() {
        // Distâncias dos sensores
        int distCentro = carSize * 2;
        int distLateral = carSize * 2;
        int distLateralDist = carSize * 2;

        //criando cada sensor
        sensorCentro = new Sensor("sensorCentro",x,y,angulo,0,distCentro,carSize);
        sensores.add(sensorCentro);
        sensorEsquerda = new Sensor("sensorEsquerda1",x,y,angulo,-Math.PI/8,distCentro,carSize);
        sensores.add(sensorEsquerda);
        sensorEsquerda = new Sensor("sensorEsquerda2",x,y,angulo,-2*Math.PI/8,distLateral,carSize);
        sensores.add(sensorEsquerda);
        sensorEsquerda = new Sensor("sensorEsquerda3",x,y,angulo,-3*Math.PI/8,distLateral,carSize);
        sensores.add(sensorEsquerda);
        sensorEsquerda = new Sensor("sensorEsquerda4",x,y,angulo,-Math.PI/2,distLateralDist,carSize);
        sensores.add(sensorEsquerda);
        sensorDireita = new Sensor("sensorDireita1",x,y,angulo,Math.PI/8,distCentro,carSize);
        sensores.add(sensorDireita);
        sensorDireita = new Sensor("sensorDireita2",x,y,angulo,2*Math.PI/8,distLateral,carSize);
        sensores.add(sensorDireita);
        sensorDireita = new Sensor("sensorDireita3",x,y,angulo,3*Math.PI/8,distLateral,carSize);
        sensores.add(sensorDireita);
        sensorDireita = new Sensor("sensorDireita4",x,y,angulo,Math.PI/2,distLateral,carSize);
        sensores.add(sensorDireita);
    }

    public void desenharCarro(Canvas canvas) {
        canvas.save();

        for(Sensor sensor : sensores){
            sensor.desenharSensor(canvas,x,y,carSize);
        }
        //Coordenadas do centro do carro
        int centroCarroX = x + carroBitmap.getWidth() / 2;
        int centroCarroY = y + carroBitmap.getHeight() / 2;

        // Rotaciona o canvas em relação ao centro do carro
        canvas.rotate((float) Math.toDegrees(angulo), centroCarroX, centroCarroY);

        // Desenha o bitmap do carro na posição original (x, y) sem translação adicional
        canvas.drawBitmap(carroBitmap, x, y, corCarro); // Remove a translação desnecessária

        canvas.restore();
    }

    private double normalizarAngulo(double angulo) {
        while (angulo > Math.PI) angulo -= 2 * Math.PI;
        while (angulo < -Math.PI) angulo += 2 * Math.PI;
        return angulo;
    }
    public int getVelocidadeMaxima() {
        return 40; // ou qualquer outro valor que represente a velocidade máxima do carro.
    }

    public int getVelocidade() {
        return velocidade;
    }
    public int getVoltasCompletadas() {
        return voltasCompletadas;
    }
    public int getAltura() {
        return altura;
    }
    public int getLargura() {
        return largura;
    }
    public boolean getCruzouLinha() {
        return cruzouLinha;
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public int getPenalidade() {
        return penalidade;
    }
    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.y = y;
    }
    public void setVoltasCompletadas(int voltasCompletadas) {
        this.voltasCompletadas = voltasCompletadas;
    }
    public void setCruzouLinha(boolean cruzouLinha) {
        this.cruzouLinha = cruzouLinha;
    }

    private void desenharCentroMassa(Canvas canvas, int centroMassaX, int centroMassaY) {
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);  // Espessura da linha do X

        int tamanhoX = 5; // Tamanho do X

        // Desenha um "X"
        canvas.drawLine(centroMassaX - tamanhoX, centroMassaY - tamanhoX, centroMassaX + tamanhoX, centroMassaY + tamanhoX, paint);
        canvas.drawLine(centroMassaX - tamanhoX, centroMassaY + tamanhoX, centroMassaX + tamanhoX, centroMassaY - tamanhoX, paint);

    }
    public void setPistaAtualizada(Bitmap pistaAtualizada) {
        this.pista = pistaAtualizada;
    }


}
