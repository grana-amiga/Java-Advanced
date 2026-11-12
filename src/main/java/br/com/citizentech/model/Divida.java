package br.com.citizentech.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "divida")
public class Divida {

    @Id
    @Column(name = "id_divida")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_divida")
    @SequenceGenerator(name = "seq_divida", sequenceName = "seq_divida", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String credor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDivida tipo;

    @Column(name = "saldo_devedor", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoDevedor;

    @Column(name = "taxa_juros_mes", nullable = false, precision = 10, scale = 4)
    private BigDecimal taxaJurosMes;

    @Column(name = "valor_parcela", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorParcela;

    @Column(name = "dia_vencimento", nullable = false)
    private Integer diaVencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDivida status;

    public Divida() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getCredor() {
        return credor;
    }

    public void setCredor(String credor) {
        this.credor = credor;
    }

    public TipoDivida getTipo() {
        return tipo;
    }

    public void setTipo(TipoDivida tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getSaldoDevedor() {
        return saldoDevedor;
    }

    public void setSaldoDevedor(BigDecimal saldoDevedor) {
        this.saldoDevedor = saldoDevedor;
    }

    public BigDecimal getTaxaJurosMes() {
        return taxaJurosMes;
    }

    public void setTaxaJurosMes(BigDecimal taxaJurosMes) {
        this.taxaJurosMes = taxaJurosMes;
    }

    public BigDecimal getValorParcela() {
        return valorParcela;
    }

    public void setValorParcela(BigDecimal valorParcela) {
        this.valorParcela = valorParcela;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public void setDiaVencimento(Integer diaVencimento) {
        this.diaVencimento = diaVencimento;
    }

    public StatusDivida getStatus() {
        return status;
    }

    public void setStatus(StatusDivida status) {
        this.status = status;
    }
}
