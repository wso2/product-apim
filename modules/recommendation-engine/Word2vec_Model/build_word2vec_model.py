import gensim

"""
The 'Dataset.txt' file consists of API descriptions of over 15,000 APIs.
Using the 'Dataset_PW.txt' file, a dataset which consists of sentences, is created.
"""

f=open("Dataset_PW.txt", "r")
contents =f.read()

dataset = []
for sentence in gensim.summarization.textcleaner.get_sentences(contents):
    sentences = list(gensim.utils.simple_preprocess (sentence))
    dataset.append(sentences)

"""
Using gensim, a word2vec model is built and trained using the above dataset.
"""
model = gensim.models.Word2Vec (dataset, size=300, window=10, min_count=5, workers=10)
model.train(dataset,total_examples=len(dataset),epochs=15)

"""
The word2vec model is saved to the directory as a .model file.
"""
model.save("word2vec_model.model")