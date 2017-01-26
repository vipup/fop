/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fopdemo.simplefop;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.FopFactoryConfig;
import org.apache.fop.apps.MimeConstants;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
// import org.apache.xmlgraphics.util.MimeConstants;  ???

/**
 *
 * @author harry
 */
public class SimpleFOPDemo implements ErrorListener
{
    public static void main(String... argv)
    {
        String inputFileName = "simple.fo";
        String outputFileName = null;
        String outputFormat = MimeConstants.MIME_PDF;   //  MimeConstants.MIME_XSL_FO
        if(argv.length > 0) {
            inputFileName = argv[0];
            if (argv.length > 1) {
                outputFileName = argv[1];
                if (argv.length > 2) {
                    outputFormat = argv[2];
                }
            }
        }
        
        SimpleFOPDemo demo = new SimpleFOPDemo();
        demo.doFopProcessing(inputFileName, outputFileName, outputFormat);
    }
        
    public void doFopProcessing(String inputFileName, String outputFileName, String outputFormat)
    {
        OutputStream out = null;
        try {
            if(outputFileName != null) {
                out = new BufferedOutputStream(new FileOutputStream(outputFileName));
            } else {
                out = new BufferedOutputStream(System.out);
            }
            
            File inputFile = new File(inputFileName);
            FopFactory fopFactory = buildFopFactory(inputFile);
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            Result res = null;
            if (!MimeConstants.MIME_XSL_FO.equals(outputFormat)) {
                Fop fop = foUserAgent.newFop(outputFormat, out);
                res = new SAXResult(fop.getDefaultHandler());
            } else {
                res = new StreamResult(out);
            }
            
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            transformer.setErrorListener(this);

            Source src = createSource(inputFile);
            transformer.transform(src, res);
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SimpleFOPDemo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FOPException ex) {
            Logger.getLogger(SimpleFOPDemo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(SimpleFOPDemo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(SimpleFOPDemo.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if(out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    Logger.getLogger(SimpleFOPDemo.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    // There is a problem in design.
    // We open an InputStream here, but we cannot close it here
    // because FOP transformer uses it....
    private static Source createSource(File inputFile)
    {
        Source source = null;
        InputStream in = null;
        try {
            String uri = null;
            if(inputFile == null) {
                in = System.in;
            } else {
                in = new FileInputStream(inputFile);
                uri = inputFile.toURI().toASCIIString();
            }
            
            InputSource is = new InputSource(in);
            is.setSystemId(uri);
            XMLReader xr = getXMLReader();
            source = new SAXSource(xr, is);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SimpleFOPDemo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SimpleFOPDemo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(SimpleFOPDemo.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            // heck.
            // temporarily commented out.
//            if(in != null) {
//                try {
//                    in.close();
//                } catch (IOException ex) {
//                    Logger.getLogger(SimpleFOPDemo.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
        }
        
        return source;
    }
    
    private static XMLReader getXMLReader() throws ParserConfigurationException, SAXException {

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://xml.org/sax/features/namespaces", true);
        spf.setFeature("http://apache.org/xml/features/xinclude", true);
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        XMLReader xr = spf.newSAXParser().getXMLReader();
        return xr;
    }

    private static URI getBaseURI(File file) 
    {
        return file == null ? new File(".").getAbsoluteFile().toURI()
                : file.getAbsoluteFile().getParentFile().toURI();
    }
    private static FopFactory buildFopFactory(File file)
    {
        FopFactoryBuilder fopFactoryBuilder;
        fopFactoryBuilder = new FopFactoryBuilder(getBaseURI(file));
        fopFactoryBuilder.setStrictFOValidation(false);
        fopFactoryBuilder.setTargetResolution(FopFactoryConfig.DEFAULT_TARGET_RESOLUTION);
        fopFactoryBuilder.setComplexScriptFeatures(false);
        return fopFactoryBuilder.build();
    }

    
    @Override
    public void warning(TransformerException exception) throws TransformerException {
        Logger.getLogger(SimpleFOPDemo.class.getName()).log(Level.WARNING, null, exception);
    }

    @Override
    public void error(TransformerException exception) throws TransformerException {
        Logger.getLogger(SimpleFOPDemo.class.getName()).log(Level.WARNING, null, exception);
    }

    @Override
    public void fatalError(TransformerException exception) throws TransformerException {
        Logger.getLogger(SimpleFOPDemo.class.getName()).log(Level.SEVERE, null, exception);
    }
}
