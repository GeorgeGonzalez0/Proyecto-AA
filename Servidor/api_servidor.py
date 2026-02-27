"""
=============================================================
  SERVIDOR FLASK - API DEL CLASIFICADOR DE FAMILIAS BIOLÓGICAS
  Proyecto Integrador - UPS Carrera Computación - Periodo 67
=============================================================
CÓMO USAR:
  1. Instala Flask: pip install flask
  2. Ejecuta: python api_servidor.py
  3. El servidor corre en http://0.0.0.0:5000
  4. La app Android llama a: http://TU_IP:5000/predict

Para obtener tu IP en Windows: ipconfig
Para obtener tu IP en Mac/Linux: ifconfig o ip addr
=============================================================
"""

from flask import Flask, request, jsonify
import pickle
import numpy as np
import os

app = Flask(__name__)

# ── Cargar modelo, scaler y encoder ──────────────────────────
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

with open(os.path.join(BASE_DIR, "modelo_familias.pkl"), "rb") as f:
    modelo = pickle.load(f)

with open(os.path.join(BASE_DIR, "label_encoder.pkl"), "rb") as f:
    le = pickle.load(f)

with open(os.path.join(BASE_DIR, "scaler.pkl"), "rb") as f:
    scaler = pickle.load(f)

FEATURES = [
    'spore_size_um', 'spore_shape', 'wall_type', 'ornamentation',
    'gene_ITS', 'genetic_cluster', 'gc_content', 'habitat_type',
    'elevation_m', 'mean_temp_c', 'pH', 'conductividad_ds_m',
    'nitrogeno_total_pct', 'textura_Arcillosa', 'textura_Arenosa',
    'textura_Franca', 'textura_Limosa'
]

print("✅ Modelo cargado correctamente")
print(f"   Familias: {list(le.classes_)}")


@app.route('/predict', methods=['POST'])
def predict():
    """
    Endpoint principal de predicción.
    Recibe JSON con las variables y devuelve la familia predicha + confianza.
    """
    try:
        data = request.get_json(force=True)

        # Validar que llegaron todos los campos
        faltantes = [f for f in FEATURES if f not in data]
        if faltantes:
            return jsonify({
                "error": f"Faltan variables: {faltantes}"
            }), 400

        # Construir vector de features
        x = np.array([[float(data[f]) for f in FEATURES]])

        # Escalar
        x_scaled = scaler.transform(x)

        # Predecir
        pred_idx = modelo.predict(x_scaled)[0]
        probas = modelo.predict_proba(x_scaled)[0]
        familia = le.inverse_transform([pred_idx])[0]
        confianza = float(probas[pred_idx])

        # Top 3 familias por probabilidad
        top3_idx = np.argsort(probas)[::-1][:3]
        top3 = [
            {
                "familia": le.inverse_transform([i])[0],
                "probabilidad": round(float(probas[i]), 4)
            }
            for i in top3_idx
        ]

        return jsonify({
            "familia_predicha": familia,
            "confianza": round(confianza, 4),
            "confianza_pct": f"{confianza*100:.1f}%",
            "top3": top3,
            "status": "ok"
        })

    except Exception as e:
        return jsonify({"error": str(e), "status": "error"}), 500


@app.route('/familias', methods=['GET'])
def get_familias():
    """Devuelve la lista de familias que el modelo puede clasificar."""
    return jsonify({
        "familias": list(le.classes_),
        "total": len(le.classes_)
    })


@app.route('/health', methods=['GET'])
def health():
    """Health check - para verificar que el servidor está activo."""
    return jsonify({"status": "ok", "mensaje": "Clasificador activo"})


if __name__ == '__main__':
    print("\n🚀 Servidor iniciando en http://0.0.0.0:5000")
    print("   Prueba en tu navegador: http://localhost:5000/health")
    print("   Para parar el servidor: Ctrl+C\n")
    app.run(host='0.0.0.0', port=5000, debug=False)
