package com.aluracursos.screenmatch.principal;

import com.aluracursos.screenmatch.model.*;
import com.aluracursos.screenmatch.repository.SerieRepository;
import com.aluracursos.screenmatch.service.ConsumoAPI;
import com.aluracursos.screenmatch.service.ConvierteDatos;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private final String URL_BASE = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=58ad1016";
    private ConvierteDatos conversor = new ConvierteDatos();
    private List<DatosSerie> datosSeries = new ArrayList<>();
    private SerieRepository repositorio;
    private List<Serie> series;
    private Optional<Serie> serieBuscada;

    public Principal(SerieRepository repository) {
        this.repositorio = repository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    1 - Buscar series
                    2 - Buscar episodios
                    3 - Mostrar series buscadas
                    4 - Buscar series por título
                    5 - Tpo 5 mejores series
                    6 - Buscar series por categoria
                    7 - Filtrar series por temporadas y evaluación
                    8 - Buscar episodios por título
                    9 - Top 5 episodios por serie
                    
                    0 - Salir
                    """;
            System.out.println(menu);
            opcion = teclado.nextInt();
            teclado.nextLine();

            switch (opcion) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    mostrarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriesPorTitulo();
                    break;
                case 5:
                    buscarTpo5Series();
                    break;
                case 6:
                    buscarSeriesPorCategoria();
                    break;
                case 7:
                    buscarSeriesPorNumeroDeTemporadasYEvaluacion();
                    break;
                case 8:
                    buscarEpisodiosPorTitulo();
                    break;
                case 9:
                    buscarTpo5Episodios();
                    break;
                case 0:
                    System.out.println("Cerrando la aplicación...");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }
    }


    private DatosSerie getDatosSerie(){
        System.out.println("Por favor escribe el nombre de la serie que deseas mostrar");
        //Busca los datos generales de las series
        var nombreSerie = teclado.nextLine();
        var json = consumoAPI.obtenerDatos(URL_BASE + nombreSerie.replace(" ", "+") + API_KEY);
        System.out.println(json);
        DatosSerie datos = conversor.obtenerDatos(json, DatosSerie.class);
        return datos;
    }

    private void buscarEpisodioPorSerie(){
        mostrarSeriesBuscadas();
        System.out.println("Escribe el nombre de la serie de la cual quieres ver los episodios");
        var nombreSerie = teclado.nextLine();

        Optional<Serie> serie = series.stream()
                .filter(s -> s.getTitulo().toLowerCase().contains(nombreSerie.toLowerCase()))
                .findFirst();

        if(serie.isPresent()){
            var serieEncontrada = serie.get();

            List<DatosTemporadas> temporadas = new ArrayList<>();
            for (int i = 1; i <= serieEncontrada.getTotalDeTemporadas(); i++){
                var json = consumoAPI.obtenerDatos(URL_BASE+serieEncontrada.getTitulo().replace(" ","+")+"&Season="+i+API_KEY);
                DatosTemporadas datosTemporadas = conversor.obtenerDatos(json, DatosTemporadas.class);
                temporadas.add(datosTemporadas);
            }
        //DatosSerie datosSerie = getDatosSerie();
        //Busca los datos de todas las temporadas
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d->d.episodios().stream()
                            .map(e -> new Episodio(d.numeroDeTemporada(),e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        }

    }

    private void buscarSerieWeb(){
        DatosSerie datos = getDatosSerie();
        Serie serie = new Serie(datos);
        repositorio.save(serie);
        //datosSeries.add(datos);
        System.out.println(datos);

    }

    private void mostrarSeriesBuscadas() {
        series = repositorio.findAll();
        series.stream().sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriesPorTitulo(){
        System.out.println("Escribe el nombre de la serie de la cual quieres buscar");
        var nombreSerie = teclado.nextLine();

        serieBuscada = repositorio.findByTituloContainsIgnoreCase(nombreSerie);

        if(serieBuscada.isPresent()){
            System.out.println("La serie buscada es: "+serieBuscada.get());
        }else{
            System.out.println("Serie no encontrada");
        }
    }

    private void buscarTpo5Series(){
        List<Serie> topSeries = repositorio.findTop5ByOrderByEvaluacionDesc();
        topSeries.forEach(s-> System.out.println("Serie: "+s.getTitulo()+" Evaluación: "+s.getEvaluacion()));
    }

    private void buscarSeriesPorCategoria(){
        System.out.println("Escribe el genero/categoria de la serie que deseas buscar");
        var genero = teclado.nextLine();
        var categoria = Categoria.fromEspanol(genero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Las series de la categoria "+genero);
        seriesPorCategoria.forEach(System.out::println);
    }

    private void buscarSeriesPorNumeroDeTemporadasYEvaluacion(){
        System.out.println("Escribe el número de cuántas temporadas tiene la serie que buscas");
        var totalTemporadas = teclado.nextInt();
        teclado.nextLine();
        System.out.println("¿Cuál es su evaluación?");
        var evaluacion = teclado.nextDouble();
        teclado.nextLine();
        List<Serie> filtroSeries = repositorio.seriesPorTemporadaYEvaluacion(totalTemporadas, evaluacion);
                //findByTotalTemporadasLessThanEqualAndEvaluacionGreaterThanEqual(totalTemporadas, evaluacion);
        System.out.println("****Series Filtradas****");
        filtroSeries.forEach(s->
                System.out.println(s.getTitulo()+" Evaluación: "+s.getEvaluacion()));
    }

    private void buscarEpisodiosPorTitulo(){
        System.out.println("Escribe el nombre del episodio que deseas buscar");
        var nombreEpisodio = teclado.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodiosPorNombre(nombreEpisodio);
        episodiosEncontrados.forEach(e->
                System.out.printf("Serie: %s - Capítulo: %s - Temporada: %s - Episodio: %s - Evaluación: %s\n",
                        e.getSerie().getTitulo(), e.getTitulo(),e.getTemporada(), e.getNumeroEpisodio(), e.getEvaluacion()));
    }

    private void buscarTpo5Episodios(){
        buscarSeriesPorTitulo();
        if (serieBuscada.isPresent()){
            Serie serie = serieBuscada.get();
            List<Episodio> topEpisodios = repositorio.top5Episodios(serie);
            topEpisodios.forEach(e->
                    System.out.printf("Serie: %s - Capítulo: %s - Temporada: %s - Episodio: %s - Evaluación: %s\n",
                            e.getSerie().getTitulo(), e.getTitulo(),e.getTemporada(), e.getNumeroEpisodio(), e.getEvaluacion()));
        }
    }
//    public static Categoria fromString(String text) {
//        for (Categoria categoria : Categoria.values()) {
//            if (categoria.categoriaOmdb.equalsIgnoreCase(text)) {
//                return categoria;
//            }
//        }
//        throw new IllegalArgumentException("Ninguna categoria encontrada: " + text);
//    }
        //temporadas.forEach(System.out::println);

        //Mostrar solo el título de los episodios para las temporadas
//        for (int i = 0; i < datos.totalDeTemporadas(); i++) {
//            List<DatosEpisodio> episodiosTemporada = temporadas.get(i).episodios();
//            for (int j = 0; j < episodiosTemporada.size(); j++) {
//                System.out.println(episodiosTemporada.get(j).titulo());
//           }
//        }
        //Funciones Lambda que sustituye a dos ciclos for anidados
//        temporadas.forEach(t -> t.episodios().forEach(e -> System.out.println(e.titulo())));

        //Convertir todas las informaciones a una lista del tipo DatosEpisodio
//        List<DatosEpisodio> datosEpisodios = temporadas.stream()
//                .flatMap(t -> t.episodios().stream())
//                .collect(Collectors.toList());

        //Top 5 episodios
//        System.out.println("Top 5 mejores episodios");
//        datosEpisodios.stream()
//                .filter(e -> !e.evaluacion().equalsIgnoreCase("N/A"))
//                .peek(e-> System.out.println("Primer filtro (N/A)" +e))
//                .sorted(Comparator.comparing(DatosEpisodio::evaluacion).reversed())
//                .peek(e-> System.out.println("Segundo ordenación (M>m)" +e))
//                .map(e->e.titulo().toUpperCase())
//                .peek(e-> System.out.println("Tercer filtro (Mayúsculas)" +e))
//                .limit(5)
//                .forEach(System.out::println);

        //Convirtiendo los datos a una lista del tipo Episodio
//        List<Episodio> episodios = temporadas.stream()
//                .flatMap(t->t.episodios().stream()
//                        .map(d->new Episodio(t.numeroDeTemporada(),d)))
//                .collect(Collectors.toList());

        //episodios.forEach(System.out::println);

        //Búsqueda de episodios a partir de x año
//        System.out.println("Por favor indica el año a partir del cual deseas ver los episodios");
//        var fecha = teclado.nextInt();
//        teclado.nextLine();

//        LocalDate fechaBusqueda = LocalDate.of(fecha, 1,1);

//        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//        episodios.stream()
//                .filter(e->e.getFechaDeLanzamiento() != null && e.getFechaDeLanzamiento().isAfter(fechaBusqueda))
//                .forEach(e-> System.out.println(
//                        "Temporada "+e.getTemporada()+
//                        " Episodio "+e.getTitulo()+
//                        " Fecha de Lanzamiento "+e.getFechaDeLanzamiento().format(dtf)
//                ));


        //Busca episodios por un pedazo del título
//        System.out.println("Ingresa el título del capitulo que deseas ver");
//        var pedazoTitulo = teclado.nextLine();
//        Optional<Episodio> episodioBuscado = episodios.stream()
//                .filter(e-> e.getTitulo().toUpperCase().contains(pedazoTitulo.toUpperCase()))
//                .findFirst();
//        if(episodioBuscado.isPresent()){
//            System.out.println("Episodio encontrado");
//            System.out.println("Los datos son: "+episodioBuscado.get());
//        }else {
//            System.out.println("Episodio no encontrado");
//        }

//        Map<Integer, Double> evaluacionesPorTemporada = episodios.stream()
//                .filter(e->e.getEvaluacion()>0.0)
//                .collect(Collectors.groupingBy(Episodio::getTemporada,
//                        Collectors.averagingDouble(Episodio::getEvaluacion)));
//        System.out.println(evaluacionesPorTemporada);
//
//        DoubleSummaryStatistics est = episodios.stream()
//                .filter(e -> e.getEvaluacion() > 0.0)
//                .collect(Collectors.summarizingDouble(Episodio::getEvaluacion));
//        System.out.println("Media de las evaluaciones: "+ est.getAverage());
//        System.out.println("Episodio mejor evaluado: " + est.getMax());
//        System.out.println("Episodio peor evaluado: " + est.getMin());







}
