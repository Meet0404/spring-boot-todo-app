export OTEL_EXPORTER_OTLP_TRACES_HEADERS="lightstep-access-token=pVAEtVzTDOatqVhZJYfRo9ywIn/jkTvspJngsgvIJBS/ojX3b6VCQA763Ttw+ngxkmS1D2EnY96GGtwrtaUiqu1N9sqGTxWuDl4l8Roq"

java -javaagent:opentelemetry-javaagent.jar \
           -Dotel.service.name=to-do-instumentation-ot \
           -Dotel.traces.exporter=logging,otlp \
           -Dotel.metrics.exporter=logging,otlp \
           -Dotel.exporter.otlp.protocol=grpc \
           -Dotel.exporter.otlp.endpoint="https://ingest.lightstep.com:443" \
           -jar path/to/your/app.jar