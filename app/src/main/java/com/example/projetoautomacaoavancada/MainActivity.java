package com.example.projetoautomacaoavancada;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Paint;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.example.bibliotecaavancada.EstadosCar;
import com.example.bibliotecaavancada.FireBase;


public class MainActivity extends Activity {
    private ImageView pistaImageView;
    private TextView cronometroTextView, voltasTextView, carrosEditText, rankingTextView;
    private Button startButton, pauseButton, finishButton, resetButton;
    private List<Car> carros = new ArrayList<>();
    private int startX = 1400;
    private int startY = 1870;
    private int carSize = 40;
    private boolean isRunning = false;
    private Bitmap pistaBitmap, mutablePistaBitmap,originalCarroBitmap,auxBitmap;
    private Canvas canvas;
    private Thread simulationThread;
    private long startTime;  // Armazena o tempo de início da corrida
    private double anguloInicial = 0;
    private List<EstadosCar> copiaEstadosCar = new ArrayList<>();

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
        resetButton = findViewById(R.id.resetButton);
        inicializarPista();
        // Carrega o bitmap aqui para garantir que não está nulo
        originalCarroBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.carro);
        auxBitmap = Bitmap.createScaledBitmap(originalCarroBitmap, carSize, carSize, false);

        startButton.setOnClickListener(v -> startRace());
        pauseButton.setOnClickListener(v -> pauseRace());
        finishButton.setOnClickListener(v -> finishRace());
        resetButton.setOnClickListener ( v-> resetRace());
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

                if (originalCarroBitmap == null) {
                    Log.e("MainActivity", "Erro ao carregar o bitmap.");
                    return; // Sai se o bitmap não for carregado
                }

                // Carregar o estado dos carros do Firestore
                copiaEstadosCar.clear();
                FireBase.carregarEstadoDosCarros(carrosRecuperados -> {
                    criarCarrosData(carrosRecuperados);
                    if (carros.isEmpty()) {
                        // Se não há carros no Firestore, cria novos carros
                        for (int i = 0; i < numCarros; i++) {
                            adicionarCarro(i);
                        }
                    } else {
                        for (Car carro : carros) {
                            // Se a thread não estiver em execução, inicie-a
                            if (!carro.isAlive()) {
                                carro.start(); // Inicia a thread do carro
                            }
                        }
                        if (carros.size() < numCarros) {
                            for (int i = carros.size(); i < numCarros; i++) {
                                adicionarCarro(i);
                            }
                        }
                    }

                    // Iniciar a simulação após adicionar ou carregar os carros
                    isRunning = true;
                    startTime = SystemClock.elapsedRealtime();
                    startSimulation();
                });
            }
        });
    }
    private void criarCarrosData(List<EstadosCar> estadosCars) {
        for (EstadosCar es : estadosCars) {

            Car novoCarro = new Car(es.getNome(), es.getX(), es.getY(), es.getCarSize(),auxBitmap,
                    es.getAngulo(),es.getVelocidade(),es.getCorOriginalCarro(),pistaBitmap,this);
            novoCarro.setVoltasCompletadas(es.getVoltasCompletadas());
            novoCarro.setDistanciaPercorrida(es.getDistanciaPercorrida());
            novoCarro.setPenalidade(es.getPenalidade());

            carros.add(novoCarro);
        }
    }
    private void adicionarCarro(int index) {
        int posX = startX - (index / 2) * 50; // Alterna entre 2 filas
        int posY = startY - (index % 2) * 30;  // Desloca para cima para a segunda fila

        //Velocidade aleatória para cada carro criado no inicio
        int velocidade = (int) (Math.random() * (40 - 15) + 15);

        //Redimensiona o bitmap do carro para o tamanho desejado
        Bitmap auxBitmap = Bitmap.createScaledBitmap(originalCarroBitmap, carSize, carSize, false);


        //Cria o objeto carro, Passando como parâmetro Nome (pega o número do for onde está sendo criado os carros), a posição inicial X e Y,

        // tamanho do carro que inicialmente pega o valor original da imagem do carro, bitmap do carro, angulo inicial e velocidade e a cor gerada aleatoriamente.

        if(index==0){
            SafetCar novoCarro = new SafetCar("Carro Seguro ", posX, posY, carSize, auxBitmap, anguloInicial,velocidade,getRandomColor(),pistaBitmap,this);
            carros.add(novoCarro);
            novoCarro.start();
        }
        Car novoCarro = new Car("Carro " + (index + 1), posX, posY, carSize, auxBitmap, anguloInicial,velocidade,getRandomColor(),pistaBitmap,this);


        //Adiciona o carro à lista de carros
        carros.add(novoCarro);
        novoCarro.start();
    }

    private void pauseRace() {
        isRunning = !isRunning;

        if (isRunning) {
            pauseButton.setText("Pause");

            // Retoma cada carro individualmente
            for (Car carro : carros) {
                carro.resumeCar(); // Retoma a thread de cada carro
            }

            startSimulation(); // Se necessário, continue a simulação (pode depender da sua lógica de início)
        } else {
            pauseButton.setText("Resume");

            // Pausa cada carro individualmente
            for (Car carro : carros) {
                carro.pauseCar(); // Pausa a thread de cada carro
            }
        }
    }

    private void finishRace() {
        isRunning = false;
        for (Car carro : carros){
            carro.interrupt();
            carro.liberarSemaforos();
            boolean isSafetCar = carro instanceof SafetCar;
            EstadosCar estadosCar = new EstadosCar(carro.getNome(),carro.getX(),carro.getY(),carro.getCarSize(),carro.getCarroBitmap(),
                    carro.getAngulo(),carro.getVelocidade(),carro.getCorOriginalCarro(),carro.getVoltasCompletadas(),
                    carro.getDistanciaPercorrida(),carro.getPenalidade(),isSafetCar);
            copiaEstadosCar.add(estadosCar);
        }

        // Limpar dados no Firestore antes de salvar novos dados
        FireBase.limparDadosCorrida(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d("Firestore", "Dados antigos limpos com sucesso!");

                    // Agora que os dados foram excluídos, podemos salvar o estado dos carros
                    FireBase.salvarEstadoDosCarros(copiaEstadosCar);

                    // Limpar a lista de carros e redesenhar a pista
                    carros.clear();
                    cronometroTextView.setText("Tempo: 00:00");
                    voltasTextView.setText("Voltas: 0");
                    rankingTextView.setText("Ranking:\n");
                    inicializarPista();
                } else {
                    Log.e("Firestore", "Erro ao limpar dados antes de salvar os novos.", task.getException());
                }
            }
        });
    }
    private void resetRace() {
        // Limpar dados no Firestore antes de salvar novos dados
        FireBase.limparDadosCorrida(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d("Firestore", "Dados antigos limpos com sucesso!");

                    // Agora que os dados foram excluídos, podemos salvar o estado dos carros

                    // Limpar a lista de carros e redesenhar a pista
                    isRunning = false;
                    carros.clear();
                    copiaEstadosCar.clear();
                    cronometroTextView.setText("Tempo: 00:00");
                    voltasTextView.setText("Voltas: 0");
                    rankingTextView.setText("Ranking:\n");
                    inicializarPista();
                } else {
                    Log.e("Firestore", "Erro ao limpar dados antes de salvar os novos.", task.getException());
                }
            }
        });

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
        for (int i = 0; i < 3; i++) {
            Car carro = carros.get(i);
            ranking.append((i + 1)).append("º ").append(carro.getNome())
                    .append(": ").append(carro.getVoltasCompletadas()).append(" voltas, Speed: ").append(carro.getVelocidade()).append("\n");
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
        canvas.drawLine(xLine, 1800, xLine, 1990, paint);
        canvas.drawLine(1000, 1800, 1000, 1990, paint2);
        canvas.drawLine(1200, 1800, 1200, 1990, paint2);

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
