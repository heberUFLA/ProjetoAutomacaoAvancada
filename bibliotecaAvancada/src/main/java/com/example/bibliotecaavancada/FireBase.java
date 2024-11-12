package com.example.bibliotecaavancada;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireBase {

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Função para salvar o estado de cada carro no Firestore
    public static void salvarEstadoDosCarros(List<EstadosCar> estadosCars) {
        for (EstadosCar es : estadosCars) {
            Map<String, Object> carData = new HashMap<>();
            carData.put("name", es.getNome());
            carData.put("x", es.getX());
            carData.put("y", es.getY());
            carData.put("tamanho", es.getCarSize());
            carData.put("carroBitmap", bitmapToBase64(es.getCarroBitmap()));
            carData.put("angulo", es.getAngulo());
            carData.put("velocidade", es.getVelocidade());
            carData.put("color", String.format("#%06X", (0xFFFFFF & es.getCorOriginalCarro())));
            carData.put("voltasCompletadas", es.getVoltasCompletadas());
            carData.put("distanciaPercorrida", es.getDistanciaPercorrida());
            carData.put("penalidade", es.getPenalidade());
            if(es.isSafetyCar()) {
                carData.put("isSafetCar", "sim");
            }else{
                carData.put("isSafetCar", "não");
            }


            db.collection("corridaEstado").document(es.getNome())
                    .set(carData)
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Car state saved: " + es.getNome()))
                    .addOnFailureListener(e -> Log.w("Firestore", "Error saving car state", e));
        }
    }

    public static void carregarEstadoDosCarros(OnCarrosLoadedCallback callback) {
        db.collection("corridaEstado")
                .get()
                .addOnCompleteListener(task -> {
                    List<EstadosCar> estadosCars = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot document : task.getResult()) {
                            // Restaure todos os atributos conforme necessário
                            String name = document.getString("name");
                            int x = document.getLong("x").intValue();
                            int y = document.getLong("y").intValue();
                            int tamanho = document.getLong("tamanho").intValue();
                            Bitmap carroBitmap = base64ToBitmap((String) document.get("carroBitmap"));
                            double angulo = document.getDouble("angulo");
                            int velocidade = document.getLong("velocidade").intValue();

                            // Obtém a cor como String e converte para int
                            String colorHex = document.getString("color");
                            int color = Color.parseColor(colorHex);

                            int voltasCompletadas = document.getLong("voltasCompletadas").intValue();
                            int distanciaPercorrida = document.getDouble("distanciaPercorrida").intValue();
                            int penalidade = document.getLong("penalidade").intValue();
                            boolean isSafetCar = false;
                            if(document.getString("isSafetCar")=="sim"){
                                isSafetCar = true;
                            }

                            EstadosCar estadosTemp = new EstadosCar(name,x,y,tamanho,carroBitmap,angulo,velocidade,color,voltasCompletadas,distanciaPercorrida,penalidade,isSafetCar);

                            estadosCars.add(estadosTemp);
                        }
                        Log.d("Firestore", "Estados dos carros carregados com sucesso.");
                        callback.onCarrosLoaded(estadosCars); // Passa a lista completa dos estados carregados
                    } else {
                        Log.w("Firestore", "Erro ao carregar estado dos carros.", task.getException());
                        callback.onCarrosLoaded(estadosCars); // Retorna lista vazia em caso de falha
                    }
                });
    }

    public interface OnCarrosLoadedCallback {
        void onCarrosLoaded(List<EstadosCar> estadosCars);
    }

    public static void limparDadosCorrida(OnCompleteListener<Void> onCompleteListener) {
        db.collection("corridaEstado")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Limpar os dados
                        db.collection("corridaEstado")
                                .get()
                                .addOnCompleteListener(deleteTask -> {
                                    if (deleteTask.isSuccessful()) {
                                        onCompleteListener.onComplete(Tasks.forResult(null));  // Dados deletados com sucesso
                                    } else {
                                        // Caso de erro, passamos a Task com erro
                                        onCompleteListener.onComplete(Tasks.forException(deleteTask.getException()));
                                    }
                                });
                    } else {
                        // Caso de erro na busca inicial
                        onCompleteListener.onComplete(Tasks.forException(task.getException()));
                    }
                });
    }
    // Converter Bitmap para String Base64
    private static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Converter String Base64 para Bitmap
    private static Bitmap base64ToBitmap(String base64Str) {
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
}

