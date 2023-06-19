from scipy.io import wavfile
import matplotlib.pyplot as plt

def process(filename):
    print("JH", filename)
    samplerate, data = wavfile.read(filename)
    print("JH", samplerate, data[0:10])

    plt.plot(data)
    plt.ylabel('wav')
    plt.savefig(filename+'.png')


    return samplerate, len(data), data