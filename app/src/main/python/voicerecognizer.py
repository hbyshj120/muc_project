import scipy
from scipy.io import wavfile
import matplotlib.pyplot as plt
from scipy import signal
import numpy as np
from scipy.fftpack import fft, fftfreq
import os.path
def process(filename):
    print("JH", filename)
    samplerate, data = wavfile.read(filename)
    print("JH", samplerate, data.shape)

    fig = plt.figure()
    plt.plot(data)
    plt.ylabel('wav')
    plt.savefig(filename+'.png')


    return samplerate, len(data), data

def corrcoeff(filename1, filename2):

    samplerate, data = wavfile.read(filename1)
    _, data2 = wavfile.read(filename2)

    # data = np.array([0, 1, 2, 3, 0, 0, 0])
    # data2 = np.array([0, 0, 1, 2, 3, 0, 0, 0, 0])
    data = data/32768.
    data2 = data2/32768.
    print("JH", samplerate, data.shape, data2.shape)

    corr = np.correlate(data ,  data2 , mode='full')
    lag = corr.argmax() - (len(data2) - 1)
    print(corr.argmax(), lag)
    plt.plot(corr)
    plt.savefig(filename2+'_corrcoeff.png')
    if lag < 0:
        data2 = data2[-lag:]
    else:
        data2 = np.pad(data2, (lag, 0), 'constant', constant_values=(0,0))

    minL = min(len(data), len(data2))
    data = data[:minL]
    data2 = data2[:minL]

    R = np.corrcoef(data, data2)
    print("R: ", R)
    plt.plot(data, 'r')
    plt.plot(data2, 'b')
    plt.savefig(filename2+'_aligned.png')

    return R[0, 1]

def fftcorrcoeff(filename1, filename2):

    samplerate, data = wavfile.read(filename1)
    _, data2 = wavfile.read(filename2)

    data = data/32768.
    data2 = data2/32768.
    print("JH", samplerate, data.shape, data2.shape)

    N = max(len(data), len(data2))
    data = np.pad(data, (N - len(data), 0), 'constant', constant_values=(0,0))
    data2 = np.pad(data2, (N - len(data2), 0), 'constant', constant_values=(0,0))

    T = 1.0/samplerate
    comp = fft(data)
    comp2 = fft(data2)
    xf = fftfreq(N, T)[:N//2]
    df = 1.0/(N*T)

    fl = 50
    fh = 5000
    nl = int(fl / df)
    nh = int(fh / df)
    print("focus on frequency range: ", xf[nl], "hz to ", xf[nh], "hz")

    R = np.corrcoef(np.abs(comp[nl:nh]), np.abs(comp2[nl:nh]))

    print("R: ", R)
    fig = plt.figure()
    plt.plot(2.0/N * np.abs(comp[nl:nh]))
    plt.plot(2.0/N * np.abs(comp2[nl:nh]))
    plt.show()

    plt.savefig(filename2+'_aligned.png')

    return R[0, 1]

def speechratio(shorts, samplerate, fl = 50., fh = 4000., threshold = 30):
    df = samplerate*1.0/len(shorts)
    [nl, nh] = [int(fl/df), int(fh/df)]

    power = np.abs(np.fft.rfft(shorts, n=len(shorts))/32768.)**2

    [speech, nonspeech] = [20*np.log10(np.sum(power[nl:nh])), 20*np.log10(np.sum(power) - np.sum(power[nl:nh]))]
    ratio = speech - nonspeech
    if 20*np.log10(np.sum(power)) < threshold:
        return 0
    # else:
    #     print("speech: ", speech, "nonspeech: ", nonspeech,  "ratio: ", ratio, 20.*np.log10(np.sum(power)))


    return ratio


def speechratios(filename, win_length = 2048, hop_length = 512):
    samplerate, y = wavfile.read(filename)

    overlap = win_length - hop_length
    rest_samples = np.abs(len(y) - overlap) % np.abs(win_length - overlap)
    pad_signal = np.append(y, np.array([0] * int(hop_length - rest_samples) * int(rest_samples != 0.)))

    nrows = ((pad_signal.size - int(win_length)) // int(hop_length)) + 1
    n = pad_signal.strides[0]
    print("n: ", n, "pad_sigal.shape", pad_signal.shape, win_length, hop_length)
    frames = np.lib.stride_tricks.as_strided(pad_signal, shape=(nrows, int(win_length)), strides=(int(hop_length*n), n))

    ratios = np.zeros(frames.shape[0])
    for i in range(frames.shape[0]):
        ratios[i] = speechratio(frames[i, :], samplerate)
        # print(i, ratios[i])
    return y, ratios

def speechcorrelation(filename1, filename2, win_length = 2048, hop_length = 512, threshold = 20):
    if not os.path.exists(filename1):
        return 0
    y1, ratio1 = speechratios(filename1, win_length = win_length, hop_length = hop_length)
    y2, ratio2 = speechratios(filename2, win_length = win_length, hop_length = hop_length)

    #################
    print("ratio1: ", ratio1)
    flag1 = np.float64(ratio1 > threshold)
    flag1 = scipy.signal.medfilt(flag1, 5)
    comb1 = flag1[:-1] + flag1[1:]
    print("comb1: ", comb1)
    index1 = np.where(comb1==1.0)[0]
    if flag1[0]:
        index1 = np.insert(index1, 0, 0)
    index1[1::2] = index1[1::2] + 1
    print("index1: ", index1)
    if len(index1) % 2 == 1:
        raise ValueError("some singular value needed to take care of in index")

    print("ratio2: ", ratio2)
    flag2 = np.float64(ratio2 > threshold)
    flag2 = scipy.signal.medfilt(flag2, 5)
    comb2 = flag2[:-1] + flag2[1:]
    if not np.any(comb2):
        # comb2 are all zeros
        return -100
    print("comb2: ", comb2)
    index2 = np.where(comb2==1.0)[0]
    if flag2[0]:
        index2 = np.insert(index2, 0, 0)
    index2[1::2] = index2[1::2] + 1
    print("index2: ", index2)
    if len(index2) % 2 == 1:
        raise ValueError("some singular value needed to take care of in index2")

    ########
    fig, (ax1, ax2) = plt.subplots(2, sharex = True)
    # fig.suptitle('check repeatability')
    ax1.plot(np.repeat(ratio1, hop_length))
    ax1.plot(y1*1.0/np.max(y1)*np.max(ratio1))
    for xc in index1:
        ax1.axvline(x = xc*hop_length)
    ax2.plot(np.repeat(ratio2, hop_length))
    ax2.plot(y2*1.0/np.max(y2)*np.max(ratio2))
    for xc in index2:
        ax2.axvline(x = xc*hop_length)
    # plt.show()
    plt.savefig(filename2+'_aligned.png')

    #################
    comb_ratio1 = np.empty(0)
    for i in range(0, len(index1), 2):
        comb_ratio1 = np.concatenate((comb_ratio1, ratio1[index1[i]:index1[i+1]+1]))

    comb_ratio2 = np.empty(0)
    for i in range(0, len(index2), 2):
        comb_ratio2 = np.concatenate((comb_ratio2, ratio2[index2[i]:index2[i+1]+1]))

    ##############
    diff = len(comb_ratio1) - len(comb_ratio2)
    left = abs(diff)//2
    right = abs(diff) - left
    print("left, right: ", left, right)

    if diff > 0:
        # 1 is longer than 2
        comb_ratio1 = np.delete(comb_ratio1, np.arange(len(comb_ratio1) - right, len(comb_ratio1)),  0)
        comb_ratio1 = np.delete(comb_ratio1, np.arange(0, left),  0)
    else:
        comb_ratio2 = np.delete(comb_ratio2, np.arange(len(comb_ratio1) - right, len(comb_ratio1)),  0)
        comb_ratio2 = np.delete(comb_ratio2, np.arange(0, left),  0)

    print(comb_ratio1, comb_ratio2)

    R = np.corrcoef(comb_ratio1, comb_ratio2)

    if np.isnan(R[0, 1]):
        R[0, 1] = 0

    print(filename1, filename2, R[0, 1])

    return int(R[0, 1] * 100)
