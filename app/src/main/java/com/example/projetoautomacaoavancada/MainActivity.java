package com.example.projetoautomacaoavancada;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class MainActivity extends Activity {
    private ImageView pistaImageView;
    private TextView cronometroTextView, voltasTextView, carrosEditText, rankingTextView;
    private Button startButton, pauseButton, finishButton;
    private List<Car> carros = new ArrayList<>();
    private int startX = 1400;
    private int startY = 1800;
    private int carSize = 40;
    private boolean isRunning = false;
    private Bitmap pistaBitmap, mutablePistaBitmap,originalCarroBitmap;
    private Canvas canvas;
    private Thread simulationThread;
    private long startTime;  // Armazena o tempo de início da corrida
    private double anguloInicial = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pistaImageView = findViewById(R.id.pistaImageView);
        cronometroTextView = findViewById(R.id.cronometroTextView);
        voltasTextView = findViewById(R.id.voltasTextView);
        carrosEditText = findViewById(R.id.carrosEditText);
        rankingTextView = findViewById(R.id.rankingTextView);


        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        finishButton = findViewById(R.id.finishButton);
        inicializarPista();

        startButton.setOnClickListener(v -> startRace());
        pauseButton.setOnClickListener(v -> pauseRace());
        finishButton.setOnClickListener(v -> finishRace());
    }

    private void startRace() {
        if (isRunning) return;

        pistaImageView.post(() -> {
            inicializarPista();
            String input = carrosEditText.getText().toString();
            int numCarros;

            if (!input.isEmpty()) {
                numCarros = Integer.parseInt(input);
                carros.clear(); // Limpa a lista de carros

                // Carrega o bitmap aqui para garantir que não está nulo
                originalCarroBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.carro);
                if (originalCarroBitmap == null) {
                    Log.e("MainActivity", "Erro ao carregar o bitmap.");
                    return; // Sai se o bitmap não for carregado
                }
                for (int i = 0; i < numCarros; i++) {
                    adicionarCarro(i);
                }

                // Iniciar a simulação após adicionar os carros
                isRunning = true;
                startTime = SystemClock.elapsedRealtime();
                startSimulation();
            }

        });
    }
    private void adicionarCarro(int index) {
        int posX = startX - (index / 2) * 50; // Alterna entre 2 filas
        int posY = startY - (index % 2) * 30;  // Desloca para cima para a segunda fila

        //Velocidade aleatória para cada carro criado no inicio
        int velocidade = (int) (Math.random() * (40 - 15) + 15);

        //Objeto onde será armazenada a cor aleatória do carro
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(getRandomColor(), PorterDuff.Mode.SRC_IN));
        //Redimensiona o bitmap do carro para o tamanho desejado
        Bitmap auxBitmap = Bitmap.createScaledBitmap(originalCarroBitmap, carSize, carSize, false);

        //Cria o objeto carro, Passando como parâmetro Nome (pega o número do for onde está sendo criado os carros), a posição inicial X e Y,
        // tamanho do carro que inicialmente pega o valor original da imagem do carro, bitmap do carro, angulo inicial e velocidade e a cor gerada aleatoriamente.
        Car novoCarro = new Car("Carro " + (index + 1), posX, posY, carSize, auxBitmap, anguloInicial,velocidade,paint,pistaBitmap,this);


        //Adiciona o carro à lista de carros
        carros.add(novoCarro);
        novoCarro.start();
    }

    private void pauseRace() {
        isRunning = !isRunning;
        if (isRunning) {
            pauseButton.setText("Pause");
            startSimulation();
        } else {
            pauseButton.setText("Resume");
        }
    }

    private void finishRace() {
        isRunning = false;
        resetarCronometro();
        carros.clear();  // Remove todos os carros da lista
        inicializarPista();  // Redesenha a pista limpa
    }

    private void startSimulation() {
        if (simulationThread == null || !simulationThread.isAlive()) {
            simulationThread = new Thread(() -> {
                while (isRunning) {
                    try {
                        Thread.sleep(30);  // Atualiza a cada 50ms
                        runOnUiThread(() -> {
                            Bitmap tempBitmap = Bitmap.createBitmap(
                                    pistaBitmap.getWidth(), pistaBitmap.getHeight(), Bitmap.Config.ARGB_8888
                            );
                            Canvas tempCanvas = new Canvas(tempBitmap);
                            tempCanvas.drawBitmap(pistaBitmap, 0, 0, null);

                            desenharLinhaChegada(tempCanvas, tempBitmap);
                            atualizarCarros(tempCanvas);

                            pistaImageView.setImageBitmap(tempBitmap);

                            for (Car carro : carros) {
                                carro.setPistaAtualizada(tempBitmap); // Adicione esse método na classe Car
                            }

                            verificarLinhaDeContagem();  // Verifica se cruzou a linha
                            atualizarCronometro();  // Atualiza cronômetro
                            atualizarRanking();
                            atualizarVoltas();
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            simulationThread.start();
        }
    }

    private void atualizarCarros(Canvas tempCanvas) {
        for (Car carro : carros) {
            // Desenha o carro na nova posição
            carro.desenharCarro(tempCanvas);
        }
    }

    private void atualizarRanking() {
        // Ordena os carros por número de voltas
        carros.sort((carro1, carro2) -> {
            return Integer.compare(carro2.getVoltasCompletadas(), carro1.getVoltasCompletadas());
        });

        // Monta o texto do ranking
        StringBuilder ranking = new StringBuilder("Ranking:\n");
        for (int i = 0; i < carros.size(); i++) {
            Car carro = carros.get(i);
            ranking.append((i + 1)).append("º ").append(carro.getName())
                    .append(": ").append(carro.getVoltasCompletadas()).append(" voltas, Speed: ").append(carro.getVelocidade()).append(", Penalidade: ").append(carro.getPenalidade()).append("\n");
        }

        rankingTextView.setText(ranking.toString());
    }

    private void atualizarVoltas() {
        // Obtém o maior número de voltas entre os carros
        int maxVoltas = carros.stream().mapToInt(carro -> carro.getVoltasCompletadas()).max().orElse(0);
        voltasTextView.setText("Voltas: " + maxVoltas);
    }

    private void atualizarCronometro() {
        long tempoDecorrido = SystemClock.elapsedRealtime() - startTime;
        int minutos = (int) (tempoDecorrido / 1000) / 60;
        int segundos = (int) (tempoDecorrido / 1000) % 60;
        String tempoFormatado = String.format("%02d:%02d", minutos, segundos);
        cronometroTextView.setText("Tempo: " + tempoFormatado);
    }

    private void resetarCronometro() {
        startTime = 0;  // Zera o tempo de início
        cronometroTextView.setText("Tempo: 00:00");  // Exibe 00:00 na tela
    }

    private void verificarLinhaDeContagem() {

        for (Car carro : carros) {
            // Se o carro passar pelo ponto de partida e completou uma volta
            if (Math.abs(carro.getX() - startX) < 40 && Math.abs(carro.getY() - startY) < 300) {
                if (!carro.getCruzouLinha()) {  // Verifica se o carro já contou uma volta nesta passagem
                    carro.setVoltasCompletadas(carro.getVoltasCompletadas() + 1);  // Incrementa o contador de voltas
                    carro.setCruzouLinha(true);  // Marca que essa volta foi contada
                }
            } else {
                carro.setCruzouLinha(false);  // Prepara para contar a próxima volta
            }
        }
    }

    private void inicializarPista() {
        pistaBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pista3);
        mutablePistaBitmap = pistaBitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(mutablePistaBitmap);
        desenharLinhaChegada(canvas, mutablePistaBitmap);
    }

    private void desenharLinhaChegada(Canvas canvas, Bitmap mutablePistaBitmap){
        // Desenhar a linha vertical amarela
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(10);
        Paint paint2 = new Paint();
        paint2.setColor(Color.GREEN);
        paint2.setStrokeWidth(10);

        int xLine = 1550;  // Coordenada X fixa para a linha vertical
        canvas.drawLine(xLine, 1700, xLine, 1900, paint);
        canvas.drawLine(900, 1700, 900, 1900, paint2);
        canvas.drawLine(1300, 1700, 1300, 1900, paint2);

        pistaImageView.setImageBitmap(mutablePistaBitmap);
    }

    private int getRandomColor() {
        Random rand = new Random();
        return Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }
    public List<Car> getCarros() {
        return carros; // Retorna a lista de carros
    }

}
