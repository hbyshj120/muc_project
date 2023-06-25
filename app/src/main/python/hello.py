from scipy.io import wavfile
import matplotlib.pyplot as plt
from scipy import signal
import numpy as np
from scipy.fftpack import fft, fftfreq

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

def speechratio(shorts, samplerate, fl = 300, fh = 3000):

    df = samplerate/len(shorts)
    nl = int(fl/df)
    nh = int(fh/df)
    # print("df: ", df, "nl: ", nl, "nh: ", nh)
    power = np.abs(np.fft.rfft(shorts, n=len(shorts)))**2.
    total = np.sum(power)
    speech = np.sum(power[nl:nh])
    ratio = speech / total
    # print("total: ", total, "speech: ", speech, "ratio: ", ratio)
    return ratio