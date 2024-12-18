package com.example.projetoautomacaoavancada;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.bibliotecaavancada.Calculos;

import java.util.concurrent.Semaphore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Car extends Thread{
    private final int corOriginalCarro;
    private String name;
    private int x;
    private int y;
    private double angulo;
    private Bitmap carroBitmap;
    private int carSize;
    protected ArrayList<Sensor> sensores = new ArrayList<>();
    private Sensor sensorCentro, sensorEsquerda, sensorDireita, sensorEsquerdaDist, sensorDireitaDist;
    protected int velocidade;
    private int voltasCompletadas=0;
    private int altura,largura;// Controle para evitar contagens repetidas
    private boolean cruzouLinha = false;// Controle de estado para a volta
    private Paint corCarro = new Paint();
    private int xAnterior, yAnterior = 0;
    private int distanciaPercorrida = 0;
    private int penalidade = 0;
    protected int velocidadeAlvo; // Velocidade que o carro tenta alcançar
    protected int velocidadeMaxima; // Defina a velocidade máxima aqui
    private Bitmap pista;
    private boolean isRunning = true;  // Variável de controle para a thread
    private MainActivity mainActivity; // Referência à MainActivity
    // Cria um semáforo com 1 permissão para acesso exclusivo
    private final Semaphore semaforo = new Semaphore(1);
    private static final Semaphore areaCriticaSemaphore = new Semaphore(1); // Limite de um carro por vez
    // Definindo os limites da área crítica (exemplo)
//    private static final int CRITICAL_AREA_START_X = 900;  // Posição X inicial
//    private static final int CRITICAL_AREA_END_X = 1300;    // Posição X final
//    private static final int CRITICAL_AREA_START_Y = 1700;   // Posição Y inicial
//    private static final int CRITICAL_AREA_END_Y = 1900;
//
    private static final int CRITICAL_AREA_START_X = 700;  // Posição X inicial
    private static final int CRITICAL_AREA_END_X = 1000;    // Posição X final
    private static final int CRITICAL_AREA_START_Y = 70;   // Posição Y inicial
    private static final int CRITICAL_AREA_END_Y = 270;
    private boolean naAreaCritica = false;// Posição Y final
    private boolean paused;
    private int distanciaAtualPercorrida = 0;
    private ExecutorService executorService;
    private int numThreads;
    private int i=0;
    private int chamadasMoverCarro=0;
    private long mediaTempoTarefa1 = 0,mediaTempoTarefa2 = 0, mediaTempoTarefa3 = 0, mediaTempoTarefa4 = 0;
    private long startTime2;
    private double segundosDecorrido = 0;


    public Car(String name, int x, int y, int carSize,Bitmap carroBitmap,
               double angulo, int velocidade,int IDcorCarro, Bitmap pista, MainActivity main) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.angulo = angulo;
        this.carroBitmap = carroBitmap;
        this.pista = pista;
        this.carSize = carSize;
        this.corOriginalCarro = IDcorCarro;
        this.corCarro.setColorFilter(new PorterDuffColorFilter(IDcorCarro, PorterDuff.Mode.SRC_IN));
        altura=carroBitmap.getHeight();
        largura=carroBitmap.getWidth();
        this.velocidade=velocidade;
        this.velocidadeAlvo = velocidade; // Inicializa a velocidade alvo com a velocidade inicial
        this.velocidadeMaxima = velocidade; // Defina a velocidade máxima aqui
        this.mainActivity = main;
        // Obtém o número de núcleos disponíveis
        numThreads = Runtime.getRuntime().availableProcessors();
        // Cria um ExecutorService com o número de threads igual ao número de núcleos
        executorService = Executors.newFixedThreadPool(numThreads);
        criarSensores();
    }
    @Override
    public void run() {
        startTime2 = System.currentTimeMillis();
        while (true) {
            try {
                synchronized (this) {
                    while (paused) {
                        wait(); // Pausa a thread enquanto o carro estiver em estado de pausa
                    }
                }
                // Verifica se o carro deve entrar na área crítica e obtém permissão
                if (naAreaCritica && saiuDaAreaCritica()) {
                    areaCriticaSemaphore.release();
                    naAreaCritica = false; // Indica que o carro saiu da área crítica
                   // Log.d("Car", "Saiu da área crítica.");
                }
                // Atualiza a posição do carro
                moverCarro();
                // Verifica colisões após a atualização de posição
                resolverColisao();
                // Verifica se o carro saiu da área crítica para liberar o semáforo
                if (!naAreaCritica && deveEntrarNaAreaCritica()) {
                    areaCriticaSemaphore.acquire();
                    naAreaCritica = true; // Indica que o carro está na área crítica
                   //Log.d("Car", "Entrou na área crítica.");
                }
                // Pausa para simular a atualização da posição em intervalos
                Thread.sleep(50);

            } catch (InterruptedException e) {
                e.printStackTrace();
                break; // Encerra a thread em caso de interrupção
            }
        }
    }

    // Métodos para controle de pausa e retomada
    public void pauseCar() {
        paused = true;
    }

    public void resumeCar() {
        paused = false;
        synchronized (this) {
            this.notify(); // Retoma a thread
        }
    }

    public void moverCarro() {
        // Tarefa 1
        executorService.submit(() -> {
            new T1(this).start();
        });

        // Tarefa 2
        executorService.submit(() -> {
            new T2(this).start();
        });

        // Tarefa 3
        executorService.submit(() -> {
            new T3(this).start();
        });

        // Tarefa 4
        executorService.submit(() -> {
            new T4(this).start();
        });
        chamadasMoverCarro++;
    }


    private boolean preverColisao() {

        int proximoX = (int) (x + Math.cos(angulo) * velocidade);
        int proximoY = (int) (y + Math.sin(angulo) * velocidade);

        if (mainActivity == null) {
            Log.e("Car", "MainActivity está nula");
            return false;
        }

        List<Car> carrosCopia;
        try {
            semaforo.acquire();
            List<Car> carros = mainActivity.getCarros();
            if (carros == null) {
                Log.e("Car", "Lista de carros está nula");
                return false;
            }

            carrosCopia = new ArrayList<>(carros);
        } catch (InterruptedException e) {
            Log.e("Car", "Erro ao adquirir semáforo: " + e.getMessage());
            return false;
        } finally {
            semaforo.release();
        }

        // Verifica a possibilidade de colisão com base na posição futura do carro
        for (Car outroCarro : carrosCopia) {
            if (outroCarro != this) {
                double distanciaFutura = Calculos.calcularDistancia(proximoX, proximoY, outroCarro.getX(), outroCarro.getY());
                if (distanciaFutura < carSize + 5) {
                    if (podeUltrapassar()) {
                        ajustarParaUltrapassagem();
                        return false;
                    }
                    velocidade = Math.max(0, velocidade - 1);
                    return true;
                }
            }
        }

        return false;
    }

    private int[] centroDeMassa(){
        List<int[]> sensoresCoords = new ArrayList<>();
        List<Integer> sensoresPesos = new ArrayList<>();
        List<Integer> pixelsPista = new ArrayList<>();

        for (Sensor sensor : sensores) {
            int[] coord = sensor.getSensorCoords(x, y, angulo, carSize, pista);
            sensoresCoords.add(coord);
            sensoresPesos.add(sensor.getDistSensorDinam());
            pixelsPista.add(pista.getPixel(coord[0], coord[1]));
        }
        return Calculos.calcularCentroMassa(sensoresCoords, sensoresPesos, x, y, carSize, pixelsPista);

    }

    private void rotacionarCarroParaCentroMassa(int[] centroMassa) {
        int centroCarroX = x + carSize / 2;
        int centroCarroY = y + carSize / 2;

        double anguloAlvo = Math.atan2(centroMassa[1] - centroCarroY, centroMassa[0] - centroCarroX);
        angulo = Calculos.normalizarAngulo(angulo);
        anguloAlvo = Calculos.normalizarAngulo(anguloAlvo);

        double diferenca = anguloAlvo - angulo;
        diferenca = Calculos.normalizarAngulo(diferenca);

        if (diferenca > Math.PI) {
            diferenca -= 2 * Math.PI;
        } else if (diferenca < -Math.PI) {
            diferenca += 2 * Math.PI;
        }

        angulo += Math.signum(diferenca) * Math.min(Math.abs(diferenca), 10);
    }

    private void ajustarParaUltrapassagem() {
        boolean esquerdaLivre = sensores.get(1).verificarSensorLivre(pista, x, y, angulo - Math.PI / 6, carSize);
        boolean direitaLivre = sensores.get(3).verificarSensorLivre(pista, x, y, angulo + Math.PI / 6, carSize);

        if (direitaLivre) {
            angulo += Math.PI / 18;
        } else if (esquerdaLivre) {
            angulo -= Math.PI / 18;
        }
    }

    private boolean podeUltrapassar() {
        boolean esquerdaLivre = sensores.get(1).verificarSensorLivre(pista, x, y, angulo - Math.PI / 6, carSize);
        boolean direitaLivre = sensores.get(3).verificarSensorLivre(pista, x, y, angulo + Math.PI / 6, carSize);
        boolean esquerdaLivre2 = sensores.get(2).verificarSensorLivre(pista, x, y, angulo - Math.PI / 6, carSize);
        boolean direitaLivre2 = sensores.get(4).verificarSensorLivre(pista, x, y, angulo + Math.PI / 6, carSize);

        return esquerdaLivre || direitaLivre || esquerdaLivre2 || direitaLivre2;
    }


    private void deadlineVolta() {
        int deadline = 22; //mili segundos
        int deslocamentoPista = 6100; //pixels

        double distanciaRestante = deslocamentoPista - distanciaAtualPercorrida;
        double velocidadePixelsPorSegundo = this.velocidade * (1 / 0.05);

        if (velocidadePixelsPorSegundo <= 0) {
            Log.e("Deadline", "Velocidade zero, carro parado.");
            return;
        }

        double tempoNecessario = distanciaRestante / velocidadePixelsPorSegundo;
        long tempoDecorrido = System.currentTimeMillis() - startTime2;
        segundosDecorrido = (double) ((tempoDecorrido / 1000)%60);


        if (tempoNecessario > deadline){
            //Log.d("Deadline", "Carro" + this.name + " estourou o deadline TempoNecessário("+tempoNecessario+"s)!");
            if(velocidade < 25) {
                velocidade += 1;
                if(velocidadeAlvo < 25){
                    velocidadeAlvo += 1;
                }
                if(velocidadeMaxima < 25){
                    velocidadeMaxima += 1;
                }

                //Log.d("Deadline", "Velocidade aumentada para " + velocidade);
            }
        }
        if(segundosDecorrido > deadline) {
            //Log.d("Deadline", "Carro" + this.name + " estourou o deadline TempoDecorrido("+segundosDecorrido+"s)!");
            if(velocidade < 25) {
                velocidade += 1;
                if(velocidadeAlvo < 25){
                    velocidadeAlvo += 1;
                }
                if(velocidadeMaxima < 25){
                    velocidadeMaxima += 1;
                }

                //Log.d("Deadline", "Velocidade aumentada para " + velocidade);
            }
        }
    }


    private void resolverColisao() {
        int distanciaMinima = carSize + 2; // Distância mínima segura entre os carros

        // Verifica se a MainActivity e a lista de carros estão acessíveis
        if (mainActivity == null) {
            Log.e("Car", "MainActivity está nula");
            return;
        }

        List<Car> carrosCopia;
        try {
            // Adquire o semáforo para acessar a lista de carros com segurança
            semaforo.acquire();
            List<Car> carros = mainActivity.getCarros();
            if (carros == null) {
                Log.e("Car", "Lista de carros está nula");
                return;
            }

            // Cria uma cópia local da lista para evitar modificações concorrentes
            carrosCopia = new ArrayList<>(carros);
        } catch (InterruptedException e) {
            Log.e("Car", "Erro ao adquirir semáforo: " + e.getMessage());
            return;
        } finally {
            semaforo.release(); // Libera o semáforo após copiar a lista
        }

        // Itera sobre os carros para verificar e resolver colisões
        for (Car outroCarro : carrosCopia) {
            if (outroCarro != this) {
                // Calcula a distância atual entre os carros
                double distanciaAtual = Calculos.calcularDistancia(this.getX(), this.getY(), outroCarro.getX(), outroCarro.getY());

                if (distanciaAtual < distanciaMinima) {
                    // Penaliza ambos os carros por colisão
                    this.penalidade++;
                    outroCarro.penalidade++;

                    // Calcula o vetor de sobreposição
                    double sobreposicao = distanciaMinima - distanciaAtual;
                    double dx = outroCarro.getX() - this.getX();
                    double dy = outroCarro.getY() - this.getY();
                    double distanciaEntreCarros = Math.sqrt(dx * dx + dy * dy);

                    if (distanciaEntreCarros > 0) {
                        // Normaliza o vetor de deslocamento
                        dx /= distanciaEntreCarros;
                        dy /= distanciaEntreCarros;

                        // Reposiciona os carros para eliminar a sobreposição
                        double deslocamentoX = dx * sobreposicao / 2.0;
                        double deslocamentoY = dy * sobreposicao / 2.0;

                        this.setX(this.getX() - (int) deslocamentoX);
                        this.setY(this.getY() - (int) deslocamentoY);

                        outroCarro.setX(outroCarro.getX() + (int)deslocamentoX);
                        outroCarro.setY(outroCarro.getY() + (int) deslocamentoY);
                    }

                    // Ajusta a velocidade do carro atual para evitar novas colisões
                    if (this.velocidade > outroCarro.velocidade) {
                        this.velocidade = Math.max(outroCarro.velocidade - 1, 0);
                    }
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

    public void calcularDistanciaPercorrida() {
        //Calcula a distancia percorrida com base no princípio da distancia entre dois pontos
        distanciaPercorrida += (int) Calculos.calcularDistancia(x, y,xAnterior,yAnterior);
    }

    private void criarSensores() {
        // Distâncias dos sensores
        int distCentro = carSize * 3;
        int distLateral = carSize * 3;
        int distLateralDist = carSize * 3;

        //criando cada sensor
        sensorCentro = new Sensor("sensorCentro",x,y,angulo,0,distCentro,carSize);
        sensores.add(sensorCentro);
        sensorEsquerda = new Sensor("sensorEsquerda1",x,y,angulo,-Math.PI/8,distCentro,carSize);
        sensores.add(sensorEsquerda);
        sensorEsquerda = new Sensor("sensorEsquerda2",x,y,angulo,-2*Math.PI/8,distLateral,carSize);
        sensores.add(sensorEsquerda);
        //sensorEsquerda = new Sensor("sensorEsquerda3",x,y,angulo,-3*Math.PI/8,distLateral,carSize);
        //sensores.add(sensorEsquerda);
        //sensorEsquerda = new Sensor("sensorEsquerda4",x,y,angulo,-Math.PI/2,distLateralDist,carSize);
        //sensores.add(sensorEsquerda);
        sensorDireita = new Sensor("sensorDireita1",x,y,angulo,Math.PI/8,distCentro,carSize);
        sensores.add(sensorDireita);
        sensorDireita = new Sensor("sensorDireita2",x,y,angulo,2*Math.PI/8,distLateral,carSize);
        sensores.add(sensorDireita);
        //sensorDireita = new Sensor("sensorDireita3",x,y,angulo,3*Math.PI/8,distLateral,carSize);
        //sensores.add(sensorDireita);
        //sensorDireita = new Sensor("sensorDireita4",x,y,angulo,Math.PI/2,distLateral,carSize);
        //sensores.add(sensorDireita);
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
        canvas.drawBitmap(carroBitmap, x, y, corCarro);

        canvas.restore();
    }

    public String getNome(){
        return name;
    }

    public int getVelocidade() {
        return velocidade;
    }
    public int getVoltasCompletadas() {
        return voltasCompletadas;
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
    public int getCorOriginalCarro() {
        return corOriginalCarro;
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

    public void liberarSemaforos() {
        if (semaforo.availablePermits() == 0) {
            semaforo.release();
        }
        if (areaCriticaSemaphore.availablePermits() == 0) {
            areaCriticaSemaphore.release();
        }

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

    public void setVelocidade(int velocidade) {
        this.velocidade = velocidade;
    }

    public void setPenalidade(int penalidade) {
        this.penalidade = penalidade;
    }

    public void setAngulo(Double angulo) {
        this.angulo = angulo;
    }

    public double getAngulo() {
        return angulo;
    }

    public void setCarId(String carId) {
        this.name = carId;
    }

    public int getCarSize() {
        return carSize;
    }

    public Bitmap getCarroBitmap() {
        return carroBitmap;
    }

    public int getDistanciaPercorrida() {
        return distanciaPercorrida;
    }

    public void setDistanciaPercorrida(int distanciaPercorrida) {
        this.distanciaPercorrida = distanciaPercorrida;
    }

    public int getOriginalColor() {
        return corOriginalCarro;
    }

    public int getNumNucleos() {
        return numThreads;
    }


    public void Tarefa1(){
        long inicioTarefa1 = System.nanoTime();
        int[] centroMassa = centroDeMassa();
        rotacionarCarroParaCentroMassa(centroMassa);
        long tempoTarefa1 = System.nanoTime() - inicioTarefa1;
        mediaTempoTarefa1 += tempoTarefa1;
    }
    public void Tarefa2(){
        long inicioTarefa2 = System.nanoTime();
        xAnterior = x;
        yAnterior = y;
        velocidade = Calculos.ajustarVelocidade(velocidade, velocidadeAlvo, velocidadeMaxima);
        long tempoTarefa2 = System.nanoTime() - inicioTarefa2;
        mediaTempoTarefa2 += tempoTarefa2;
    }
    public void Tarefa3(){
        long inicioTarefa3 = System.nanoTime();
        if (!preverColisao()) {
            x += Math.cos(angulo) * velocidade;
            y += Math.sin(angulo) * velocidade;
        } else {
            ajustarParaUltrapassagem();
        }
        long tempoTarefa3 = System.nanoTime() - inicioTarefa3;
        mediaTempoTarefa3 += tempoTarefa3;
    }
    public void Tarefa4(){
        long inicioTarefa4 = System.nanoTime();
        int auxDistanciaPercorrida = distanciaPercorrida;
        calcularDistanciaPercorrida();
        distanciaAtualPercorrida = distanciaPercorrida - auxDistanciaPercorrida;
        deadlineVolta();
        long tempoTarefa4 = System.nanoTime() - inicioTarefa4;
        mediaTempoTarefa4 += tempoTarefa4;
    }

    public long getTempoTarefa4() {
        return mediaTempoTarefa4/chamadasMoverCarro;
    }
    public long getTempoTarefa3() {
        return mediaTempoTarefa3/chamadasMoverCarro;
    }
    public long getTempoTarefa2() {
        return mediaTempoTarefa2/chamadasMoverCarro;
        }
    public long getTempoTarefa1() {
        return mediaTempoTarefa1/chamadasMoverCarro;
    }
    public double getSegundosDecorrido() {
        return segundosDecorrido;
    }
    public void setStartTime2(long l) {
        startTime2 = l;
    }
}

class T1 extends Thread {
    private Car carro;

    public T1(Car carro) {
        this.carro = carro;
    }
    @Override
    public void run() {
        carro.Tarefa1();
    }
}
class T2 extends Thread {
    private Car carro;

    public T2(Car carro) {
        this.carro = carro;
    }
    @Override
    public void run() {
        carro.Tarefa2();
    }
}
class T3 extends Thread {
    private Car carro;

    public T3(Car carro) {
        this.carro = carro;
    }
    @Override
    public void run() {
        carro.Tarefa3();
    }
}
class T4 extends Thread {
    private Car carro;

    public T4(Car carro) {
        this.carro = carro;
    }
    @Override
    public void run() {
        carro.Tarefa4();
    }
}


