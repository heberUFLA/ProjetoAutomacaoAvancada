package com.example.projetoautomacaoavancada;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Base64;
import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CorridaManager {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Função para salvar o estado de cada carro no Firestore
    public void salvarEstadoDosCarros(List<Car> carros) {
        for (Car carro : carros) {
            Map<String, Object> carData = new HashMap<>();
            carData.put("name", carro.getName());
            carData.put("x", carro.getX());
            carData.put("y", carro.getY());
            carData.put("tamanho", carro.getCarSize());
            carData.put("carroBitmap", bitmapToBase64(carro.getCarroBitmap()));
            carData.put("angulo", carro.getAngulo());
            carData.put("velocidade", carro.getVelocidade());
            carData.put("corCarro", paintToHex(carro.getCorCarro())); // Salvar cor como hexadecimal
            carData.put("voltasCompletadas", carro.getVoltasCompletadas());
            carData.put("distanciaPercorrida", carro.getDistanciaPercorrida());
            carData.put("penalidade", carro.getPenalidade());

            db.collection("corridaEstado").document(carro.getName())
                    .set(carData)
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Car state saved: " + carro.getName()))
                    .addOnFailureListener(e -> Log.w("Firestore", "Error saving car state", e));
        }
    }

    public void carregarEstadoDosCarros(OnCarrosLoadedCallback callback) {
        db.collection("corridaEstado")
                .get()
                .addOnCompleteListener(task -> {
                    List<Car> cars = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {

                        for (DocumentSnapshot document : task.getResult()) {
                            // Restaure todos os atributos conforme necessário
                            String name = document.getString("name");
                            int x = document.getLong("x").intValue();
                            int y = document.getLong("y").intValue();
                            int tamanho = document.getLong("tamanho").intValue();

                            // Verifica se o valor de "carroBitmap" não é null
                            String carroBitmapStr = document.getString("carroBitmap");
                            Bitmap carroBitmap = null;
                            if (carroBitmapStr != null) {
                                carroBitmap = base64ToBitmap(carroBitmapStr);
                            }

                            double angulo = document.getDouble("angulo");
                            int velocidade = document.getLong("velocidade").intValue();
                            Paint corCarro = hexToPaint(document.getString("corCarro"));
                            Bitmap pista = base64ToBitmap((String) document.get("pista"));
                            MainActivity mainActivity = (MainActivity) document.get("mainActivity");
                            int voltasCompletadas = document.getLong("voltasCompletadas").intValue();
                            int distanciaPercorrida = document.getDouble("distanciaPercorrida").intValue();
                            int penalidade = document.getLong("penalidade").intValue();

                            Car carro = new Car(name, x, y, tamanho, carroBitmap, angulo, velocidade, corCarro, pista, mainActivity);
                            carro.setVoltasCompletadas(voltasCompletadas);
                            carro.setDistanciaPercorrida(distanciaPercorrida);
                            carro.setPenalidade(penalidade);
                            cars.add(carro);
                        }
                        Log.d("Firestore", "Estados dos carros carregados com sucesso.");
                        callback.onCarrosLoaded(cars);
                    } else {
                        Log.w("Firestore", "Erro ao carregar estado dos carros.", task.getException());
                        callback.onCarrosLoaded(new ArrayList<>()); // Retorna lista vazia em caso de falha
                    }
                });
    }




    public interface OnCarrosLoadedCallback {
        void onCarrosLoaded(List<Car> carros);
    }

    // Converter Bitmap para String Base64
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Converter String Base64 para Bitmap
    private Bitmap base64ToBitmap(String base64Str) {
        if (base64Str == null || base64Str.isEmpty()) {
            Log.w("Base64", "A string Base64 está vazia ou null.");
            return null; // Retorna null se a string estiver vazia ou null
        }

        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            Log.e("Base64", "Erro ao decodificar Base64: " + e.getMessage());
            return null; // Retorna null em caso de erro na decodificação
        }
    }


    // Converter Paint para String hexadecimal
    private String paintToHex(Paint paint) {
        return String.format("#%06X", (0xFFFFFF & paint.getColor()));
    }

    // Converter String hexadecimal para Paint
    private Paint hexToPaint(String hexColor) {
        Paint paint = new Paint();
        paint.setColor(Color.parseColor(hexColor));
        return paint;
    }
}
