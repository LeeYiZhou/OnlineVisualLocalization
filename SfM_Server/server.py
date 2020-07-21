# -*-coding:utf-8-*-
from flask import Flask, render_template, jsonify
from flask import request, send_from_directory
import os
from werkzeug.utils import secure_filename
from colmap_3d import feature_extractor, feature_matching, sfm, result_to_ply
from image_retrieval import image_retrieval

app = Flask(__name__)
basedir = os.path.abspath(os.path.dirname(__file__))
app.config['SQLALCHEMY_COMMIT_ON_TEARDOWN'] = True
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = True


@app.route('/')
def test():
    return 'Server is running...'


ALLOWED_EXTENSIONS = {'png', 'jpg', 'JPG', 'PNG', 'bmp'}


def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1] in ALLOWED_EXTENSIONS


@app.route('/upload_image', methods=['POST'])
def upload_image():
    f = request.files['theFile']
    if not (f and allowed_file(f.filename)):
        return jsonify({"error": 1001, "msg": f"Available image type: {ALLOWED_EXTENSIONS}"})
    basepath = os.path.dirname(__file__)
    upload_path = os.path.join(basepath, 'static/images', secure_filename(f.filename))
    f.save(upload_path)
    return render_template('upload_ok.html')


@app.route('/download_ply', methods=['GET'])
def download_ply():
    return send_from_directory('static', 'target.ply', as_attachment=True)


@app.route('/start_sfm', methods=['GET'])
def start_sfm():
    database_path = 'static'

    feature_extractor(database_path, 3000)
    feature_matching(database_path)
    sfm(database_path)

    sparse_path = os.path.join(database_path, 'sparse', '0')
    result_to_ply(sparse_path, database_path)

    return "reconstruction is successful"


@app.route('/visual_localization', methods=['POST'])
def visual_localization():
    f = request.files['theFile']
    if not (f and allowed_file(f.filename)):
        return jsonify({"error": 1001, "msg": f"Available image type: {ALLOWED_EXTENSIONS}"})
    basepath = os.path.dirname(__file__)
    upload_path = os.path.join(basepath, 'static/query_images', secure_filename(f.filename))
    f.save(upload_path)

    xyz, match_score = image_retrieval(upload_path, 'static/images/')
    xyz = [str(item) for item in xyz]
    print('Query match score: ', match_score)

    return ' '.join(xyz)


if __name__ == '__main__':
    app.run(host='127.0.0.1')
