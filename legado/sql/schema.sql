CREATE TABLE usuario (
    usuario_id SERIAL NOT NULL,
    login VARCHAR(50) NOT NULL,
    nome VARCHAR(120) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL DEFAULT 'OPERADOR',
    ativo CHAR(1) NOT NULL DEFAULT 'S',
    data_cadastro DATE NOT NULL DEFAULT TODAY,
    CONSTRAINT pk_usuario PRIMARY KEY (usuario_id),
    CONSTRAINT uq_usuario_login UNIQUE (login),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMIN','OPERADOR','CONSULTA')),
    CONSTRAINT ck_usuario_ativo CHECK (ativo IN ('S','N'))
);

CREATE TABLE cliente (
    cliente_id SERIAL NOT NULL,
    razao_social VARCHAR(150) NOT NULL,
    nome_fantasia VARCHAR(150),
    documento VARCHAR(20) NOT NULL,
    email VARCHAR(120),
    telefone VARCHAR(30),
    endereco VARCHAR(200),
    cidade VARCHAR(80),
    uf CHAR(2),
    cep VARCHAR(10),
    ativo CHAR(1) NOT NULL DEFAULT 'S',
    data_cadastro DATE NOT NULL DEFAULT TODAY,
    CONSTRAINT pk_cliente PRIMARY KEY (cliente_id),
    CONSTRAINT uq_cliente_documento UNIQUE (documento),
    CONSTRAINT ck_cliente_ativo CHECK (ativo IN ('S','N'))
);

CREATE TABLE produto (
    produto_id SERIAL NOT NULL,
    codigo VARCHAR(30) NOT NULL,
    descricao VARCHAR(150) NOT NULL,
    unidade_medida VARCHAR(10) NOT NULL DEFAULT 'UN',
    preco_venda DECIMAL(12,2) NOT NULL,
    estoque_atual DECIMAL(12,3) NOT NULL DEFAULT 0,
    ativo CHAR(1) NOT NULL DEFAULT 'S',
    CONSTRAINT pk_produto PRIMARY KEY (produto_id),
    CONSTRAINT uq_produto_codigo UNIQUE (codigo),
    CONSTRAINT ck_produto_preco CHECK (preco_venda >= 0),
    CONSTRAINT ck_produto_estoque CHECK (estoque_atual >= 0),
    CONSTRAINT ck_produto_ativo CHECK (ativo IN ('S','N'))
);

CREATE TABLE pedido (
    pedido_id SERIAL NOT NULL,
    cliente_id INTEGER NOT NULL,
    usuario_id INTEGER NOT NULL,
    data_pedido DATE NOT NULL DEFAULT TODAY,
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTO',
    valor_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    observacao VARCHAR(500),
    CONSTRAINT pk_pedido PRIMARY KEY (pedido_id),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id),
    CONSTRAINT fk_pedido_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (usuario_id),
    CONSTRAINT ck_pedido_status CHECK (status IN ('ABERTO','CONFIRMADO','FATURADO','CANCELADO')),
    CONSTRAINT ck_pedido_total CHECK (valor_total >= 0)
);

CREATE TABLE item_pedido (
    item_pedido_id SERIAL NOT NULL,
    pedido_id INTEGER NOT NULL,
    produto_id INTEGER NOT NULL,
    quantidade DECIMAL(12,3) NOT NULL,
    preco_unitario DECIMAL(12,2) NOT NULL,
    desconto DECIMAL(12,2) NOT NULL DEFAULT 0,
    valor_total DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_item_pedido PRIMARY KEY (item_pedido_id),
    CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (pedido_id),
    CONSTRAINT fk_item_pedido_produto FOREIGN KEY (produto_id) REFERENCES produto (produto_id),
    CONSTRAINT uq_item_pedido UNIQUE (pedido_id, produto_id),
    CONSTRAINT ck_item_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_item_preco CHECK (preco_unitario >= 0),
    CONSTRAINT ck_item_desconto CHECK (desconto >= 0),
    CONSTRAINT ck_item_total CHECK (valor_total >= 0)
);

CREATE INDEX ix_pedido_cliente ON pedido (cliente_id);
CREATE INDEX ix_pedido_usuario ON pedido (usuario_id);
CREATE INDEX ix_item_pedido_pedido ON item_pedido (pedido_id);
