package net.elpuig.Practica7b.m7.servlets;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import net.elpuig.Practica7b.m7.beans.Alumno;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;
import net.sf.jasperreports.j2ee.servlets.ImageServlet;
import net.sf.jasperreports.web.util.WebHtmlResourceHandler;

public class Report extends HttpServlet {
private static final long serialVersionUID = 1L;

protected void service(HttpServletRequest request, HttpServletResponse response)
throws ServletException, IOException {
final AsyncContext ctxAsincrono = request.startAsync();
ctxAsincrono.setTimeout(10_0000); // 10 segundos como máximo para generar el report

ctxAsincrono.addListener(new AsyncListener() {
public void onComplete(AsyncEvent event) throws IOException {
System.out.println("Informe generado");
}
public void onTimeout(AsyncEvent event) throws IOException {
System.out.println("Se ha excedido el tiempo máximo para generar el informe");
}
public void onError(AsyncEvent event) throws IOException {
System.out.println(
"Se ha producido un error al generar el informe: "
+ event.getThrowable().getMessage());
}
public void onStartAsync(AsyncEvent event) throws IOException {
}
});
ctxAsincrono.start(new Runnable() {
@Override
public void run() {
try {
// Localizar el informe compilado
File informeCompilado = new File(
getServletContext().getRealPath("/WEB-INF/informes/alumnos/Alumnos.jasper"));

// Cargar el informe compilado
JasperReport jasperReport = (JasperReport) JRLoader
.loadObject(new File(informeCompilado.getPath()));

// Recuperar la lista de alumnos de la solicitud o cargarla si no existe
Collection<Alumno> listaAlumnos;
Object listaObj = request.getAttribute("lista");
if (listaObj instanceof Collection<?>) {
    Collection<?> tempCollection = (Collection<?>) listaObj;
    boolean allElementsAreAlumno = tempCollection.stream().allMatch(element -> element instanceof Alumno);
    if (allElementsAreAlumno) {
        listaAlumnos = tempCollection.stream().map(element -> (Alumno) element).collect(Collectors.toList());
    } else {
        throw new IllegalArgumentException("The collection does not contain only Alumno objects.");
    }
} else {
    // Si no hay lista en la request, obtenemos todos los alumnos
    listaAlumnos = Alumno.load();
}

// Crear el datasource con la colección de beans
JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(listaAlumnos);

// Actualizarlo con los datos
JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(0), dataSource);

/*
* Ahora ejecutamos un método específico según el informe que haya seleccionado
* el usuario
*/
// Obtener el tipo de informe seleccionado por el usuario
String tipoInforme = request.getParameter("optInformes"); 
response.setContentType(tipoInforme);

if ("application/pdf".equals(tipoInforme)) {
    JRPdfExporter exporter = new JRPdfExporter();
    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream()));
    SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
    exporter.setConfiguration(configuration);
    exporter.exportReport();
} else if ("application/vnd.ms-excel".equals(tipoInforme)) {
    response.setHeader("Content-Disposition", "inline; filename=informe.xls");
    JRXlsExporter exporter = new JRXlsExporter();
    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream()));
    SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
    exporter.setConfiguration(configuration);
    exporter.exportReport();
} else if ("application/msword".equals(tipoInforme)) {
    response.setHeader("Content-Disposition", "inline; filename=informe.doc");
    JRDocxExporter exporter = new JRDocxExporter();
    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream()));
    exporter.exportReport();
} else {
    request.getSession().setAttribute(ImageServlet.DEFAULT_JASPER_PRINT_SESSION_ATTRIBUTE, jasperPrint);
    HtmlExporter exporter = new HtmlExporter();
    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
    SimpleHtmlExporterOutput exporterOutput = new SimpleHtmlExporterOutput(response.getOutputStream());
    exporterOutput.setImageHandler(new WebHtmlResourceHandler("image?image={0}"));
    exporter.setExporterOutput(exporterOutput);
    exporter.exportReport();
}
} catch (JRException | IOException e) {
e.printStackTrace();
} finally {
ctxAsincrono.complete();
}
}
});
}
}