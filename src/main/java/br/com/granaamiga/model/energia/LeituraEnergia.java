package br.com.granaamiga.model.energia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leitura_energia")
public class LeituraEnergia {

    @Id
    @Column(name = "id_leitura")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_leitura")
    @SequenceGenerator(name = "seq_leitura", sequenceName = "seq_leitura_energia", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dispositivo", nullable = false)
    private Dispositivo dispositivo;

    @Column(name = "dt_leitura", nullable = false)
    private LocalDateTime dataLeitura;

    @Column(name = "potencia_w", nullable = false, precision = 15, scale = 3)
    private BigDecimal potenciaW;

    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal kwh;

    @Column(name = "custo_estimado", nullable = false, precision = 19, scale = 2)
    private BigDecimal custoEstimado;

    public LeituraEnergia() {
    }

    @PrePersist
    public void preencherDataLeitura() {
        if (dataLeitura == null) {
            dataLeitura = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dispositivo getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(Dispositivo dispositivo) {
        this.dispositivo = dispositivo;
    }

    public LocalDateTime getDataLeitura() {
        return dataLeitura;
    }

    public void setDataLeitura(LocalDateTime dataLeitura) {
        this.dataLeitura = dataLeitura;
    }

    public BigDecimal getPotenciaW() {
        return potenciaW;
    }

    public void setPotenciaW(BigDecimal potenciaW) {
        this.potenciaW = potenciaW;
    }

    public BigDecimal getKwh() {
        return kwh;
    }

    public void setKwh(BigDecimal kwh) {
        this.kwh = kwh;
    }

    public BigDecimal getCustoEstimado() {
        return custoEstimado;
    }

    public void setCustoEstimado(BigDecimal custoEstimado) {
        this.custoEstimado = custoEstimado;
    }
}
