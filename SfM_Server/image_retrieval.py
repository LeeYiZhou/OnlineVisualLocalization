import os
import pickle
import cv2
import imageio
import numpy as np
import scipy
import scipy.spatial
from pylab import plt
import random
from static.read_write_model import read_model


# Feature extractor
def extract_features(image_path, vector_size=32):
    image = imageio.imread(image_path)
    try:
        # SIFT feature extraction by OpenCV
        alg = cv2.xfeatures2d.SIFT_create()
        # Dinding image keypoints
        kps = alg.detect(image)
        # Getting first 32 of them.
        # Number of keypoints is varies depend on image size and color pallet
        # Sorting them based on keypoint response value(bigger is better)
        kps = sorted(kps, key=lambda x: -x.response)[:vector_size]
        # computing descriptors vector
        kps, dsc = alg.compute(image, kps)
        # Flatten all of them in one big vector - our feature vector
        dsc = dsc.flatten()
        # Making descriptor of same size
        # Descriptor vector size is 64
        needed_size = (vector_size * 64)
        if dsc.size < needed_size:
            # if we have less the 32 descriptors then just adding zeros at the
            # end of our feature vector
            dsc = np.concatenate([dsc, np.zeros(needed_size - dsc.size)])
    except cv2.error as e:
        print ('Error: ', e)
        return None

    return dsc


def batch_extractor(images_path, pickled_db_path="features.pck"):
    files = [os.path.join(images_path, p) for p in sorted(os.listdir(images_path))]

    result = {}
    for f in files:
        print('Extracting features from image %s' % f)
        name = f.split('/')[-1].lower()
        result[name] = extract_features(f)

    with open(pickled_db_path, 'wb') as fb:
        # dump the feature as a file, preventing feature extraction next time
        pickle.dump(result, fb)

    return result


class Matcher(object):

    def __init__(self, pickled_db_path="features.pck"):
        with open(pickled_db_path, 'rb') as fb:
            self.data = pickle.load(fb)
        self.names = []
        self.matrix = []
        for k, v in self.data.items():
            self.names.append(k)
            self.matrix.append(v)
        self.matrix = np.array(self.matrix)
        self.names = np.array(self.names)

    def cos_cdist(self, vector):
        # getting cosine distance between search image and images database
        v = vector.reshape(1, -1)
        return scipy.spatial.distance.cdist(self.matrix, v, 'cosine').reshape(-1)

    def match(self, image_path, topn=5):
        features = extract_features(image_path)
        img_distances = self.cos_cdist(features)
        # getting top 5 records
        nearest_ids = np.argsort(img_distances)[:topn].tolist()
        nearest_img_paths = self.names[nearest_ids].tolist()

        return nearest_img_paths, img_distances[nearest_ids].tolist()


def show_img(path):
    img = imageio.imread(path)
    plt.imshow(img)
    plt.show()


def image_retrieval(source_image_path, target_dir):
    if not os.path.exists('features.pck'):
        batch_extractor(target_dir)

    ma = Matcher()

    names, matches = ma.match(source_image_path, topn=3)

    best_match_name = names[0]
    match_ratio = 1 - matches[0]

    _, images, _ = read_model('static/sparse/0', ext='.bin')

    for idx in images.keys():
        image = images[idx]

        print(image[4].lower(), best_match_name.lower())

        if best_match_name.lower() == image[4].lower():
            return image[2], match_ratio

    assert 'No corresponding image found'


def run():
    images_path = 'static/images/'
    files = [os.path.join(images_path, p) for p in sorted(os.listdir(images_path))]
    # getting 3 random images
    sample = random.sample(files, 3)

    batch_extractor(images_path)

    ma = Matcher()

    for s in sample:
        print('Query image ==========================================')
        show_img(s)
        names, match = ma.match(s, topn=3)
        print('Result images ========================================')
        for i in range(3):
            print('Match %s' % (1 - match[i]))
            show_img(os.path.join(images_path, names[i]))


if __name__ == '__main__':
    images_path = 'static/images/'
    files = [os.path.join(images_path, p) for p in sorted(os.listdir(images_path))]

    source_path = 'static/P1180162.JPG'
    matched_file_path, score = image_retrieval(source_path, images_path)

    print(matched_file_path, score)

