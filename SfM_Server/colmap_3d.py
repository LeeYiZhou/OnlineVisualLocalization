import os

from static.read_write_model import read_model

# please change "C:/Users/Administrator/PycharmProjects/SfM_Server/sfm/" to the absolute directory of COLMAP.bat
COLMAP = 'C:/Users/Administrator/PycharmProjects/SfM_Server/sfm/colmap'
print(os.listdir())
EXTRACT_FEATURE = ' feature_extractor '

MATCH_FEATURE = ' exhaustive_matcher '
SFM = ' mapper '


def feature_extractor(database_path, max_features=2000):
    cmd = COLMAP + EXTRACT_FEATURE + f'--database_path {database_path}/database.db ' \
                                     f'--image_path {database_path}/images ' \
                                     f'--SiftExtraction.max_num_features {max_features} ' \
                                     f'--SiftExtraction.use_gpu no'
    print(cmd)
    d = os.system(cmd)
    print(d)


def feature_matching(database_path):
    cmd = COLMAP + MATCH_FEATURE + f'--database_path {database_path}/database.db ' \
                                   f'--SiftMatching.use_gpu no'
    d = os.system(cmd)
    print(cmd)
    print(d)


def sfm(database_path):
    mkdir_cmd = f'mkdir {database_path}\sparse'
    print(mkdir_cmd)
    d = os.system(mkdir_cmd)
    print(f'mkdir: {d}')

    sfm_cmd = COLMAP + SFM + f'--database_path {database_path}/database.db ' \
                             f'--image_path {database_path}/images ' \
                             f'--output_path {database_path}/sparse '
    print(sfm_cmd)
    d = os.system(sfm_cmd)
    print(f'sfm: {d}')


def result_to_ply(sparse_path, target_path):
    _, _, points3D = read_model(sparse_path, ext='.bin')
    target_ply_path = os.path.join(target_path, 'target.ply')
    with open(target_ply_path, 'w', encoding='utf8') as fw:
        nb_points = len(points3D.keys())
        fw.write(f'element vertex {nb_points}\nend_header\n')
        for idx in points3D.keys():
            point = points3D[idx]
            coordinate = point[1]
            color = point[2]
            normal = [0, 0, 0]

            str_to_write = '%.2fm%.2fm%.2fm%dm%dm%dm%dm%dm%dm%d\n' % (coordinate[0], coordinate[1], coordinate[2],
                                                                normal[0], normal[1], normal[2],
                                                                color[0], color[1], color[2], 255)
            fw.write(str_to_write)


if __name__ == '__main__':
    database_path = 'static'
    #
    # feature_extractor(database_path)
    # feature_matching(database_path)
    # sfm(database_path)

    # cameras, images, points3D = read_model('static/sparse/0', ext='.bin')
    # print(points3D[2])

    result_to_ply('static/sparse/0', 'static/')
