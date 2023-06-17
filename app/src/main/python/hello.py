from scipy.io import wavfile
def process(filename):
    print("JH", filename)
    samplerate, data = wavfile.read(filename)
    print("JH", samplerate, data[0:10])
    return 0