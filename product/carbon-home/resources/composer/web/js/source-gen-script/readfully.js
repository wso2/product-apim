function readFully(url) {
    var result = "";
    var imports = new JavaImporter(java.net, java.lang, java.io);

    with (imports) {

        var urlObj = null;

        try {
            urlObj = new URL(url);
        } catch (e) {
            // If the URL cannot be built, assume it is a file path.
            urlObj = new URL(new File(url).toURI().toURL());
        }

        var reader = new BufferedReader(new InputStreamReader(urlObj.openStream()));

        var line = reader.readLine();
        while (line != null) {
            result += line + "\n";
            line = reader.readLine();
        }

        reader.close();
    }

    return result;
}