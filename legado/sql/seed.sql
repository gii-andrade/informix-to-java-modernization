-- Massa de dados fictícia para o Sistema de Pedidos.
-- Execute após legado/sql/schema.sql em um banco vazio.

BEGIN WORK;

INSERT INTO usuario
    (usuario_id, login, nome, senha_hash, perfil, ativo, data_cadastro)
VALUES
    (1, 'admin', 'Mariana Costa', '$2a$10$K9YhGZz6mA7R9t5nB1cDMeR9sM4E6rT2bQ8vP3kL7xN5wF1aH0jC', 'ADMIN', 'S', MDY(1, 5, 2026));
INSERT INTO usuario
    (usuario_id, login, nome, senha_hash, perfil, ativo, data_cadastro)
VALUES
    (2, 'carlos.silva', 'Carlos Eduardo Silva', '$2a$10$V8kP3sH1nQ5eR7tY9uI2oL6cD4bA0mN8xW3zF5gJ1hK7pQ9rS2tU', 'OPERADOR', 'S', MDY(1, 8, 2026));
INSERT INTO usuario
    (usuario_id, login, nome, senha_hash, perfil, ativo, data_cadastro)
VALUES
    (3, 'ana.lima', 'Ana Beatriz Lima', '$2a$10$R6mN2pV8wX4yZ1aB5cD7eF9gH3iJ0kL6qS2tU4vW8xY1zA5bC7dE', 'CONSULTA', 'S', MDY(2, 3, 2026));

INSERT INTO cliente
    (cliente_id, razao_social, nome_fantasia, documento, email, telefone,
     endereco, cidade, uf, cep, ativo, data_cadastro)
VALUES
    (1, 'Almeida & Filhos Comércio Ltda.', 'Almeida Materiais', '12.345.678/0001-90',
     'compras@almeidamateriais.com.br', '(11) 3456-1200', 'Rua das Acácias, 245',
     'São Paulo', 'SP', '04567-120', 'S', MDY(1, 10, 2026));
INSERT INTO cliente
    (cliente_id, razao_social, nome_fantasia, documento, email, telefone,
     endereco, cidade, uf, cep, ativo, data_cadastro)
VALUES
    (2, 'Tecnologia Horizonte S.A.', 'Horizonte Tech', '45.678.901/0001-23',
     'suprimentos@horizontetech.com.br', '(21) 3201-7788', 'Avenida Atlântica, 980',
     'Rio de Janeiro', 'RJ', '22010-000', 'S', MDY(1, 15, 2026));
INSERT INTO cliente
    (cliente_id, razao_social, nome_fantasia, documento, email, telefone,
     endereco, cidade, uf, cep, ativo, data_cadastro)
VALUES
    (3, 'Café do Vale Indústria e Comércio Ltda.', 'Café do Vale', '78.901.234/0001-56',
     'financeiro@cafedovale.com.br', '(31) 3344-5620', 'Rodovia MG-010, Km 18',
     'Belo Horizonte', 'MG', '31630-900', 'S', MDY(2, 1, 2026));

INSERT INTO produto
    (produto_id, codigo, descricao, unidade_medida, preco_venda, estoque_atual, ativo)
VALUES
    (1, 'NOTE-PRO-14', 'Notebook Pro 14 polegadas, 16 GB RAM, 512 GB SSD', 'UN', 4899.90, 18, 'S');
INSERT INTO produto
    (produto_id, codigo, descricao, unidade_medida, preco_venda, estoque_atual, ativo)
VALUES
    (2, 'MON-27-QHD', 'Monitor 27 polegadas QHD IPS', 'UN', 1599.00, 35, 'S');
INSERT INTO produto
    (produto_id, codigo, descricao, unidade_medida, preco_venda, estoque_atual, ativo)
VALUES
    (3, 'DOCK-USBC', 'Dock USB-C com HDMI e rede Gigabit', 'UN', 649.90, 42, 'S');
INSERT INTO produto
    (produto_id, codigo, descricao, unidade_medida, preco_venda, estoque_atual, ativo)
VALUES
    (4, 'CABO-HDMI-2M', 'Cabo HDMI 2.0 de 2 metros', 'UN', 49.90, 120, 'S');
INSERT INTO produto
    (produto_id, codigo, descricao, unidade_medida, preco_venda, estoque_atual, ativo)
VALUES
    (5, 'TECL-MEC-ABNT', 'Teclado mecânico ABNT2 com iluminação', 'UN', 329.90, 24, 'S');

INSERT INTO pedido
    (pedido_id, cliente_id, usuario_id, data_pedido, status, valor_total, observacao)
VALUES
    (1, 1, 2, MDY(6, 3, 2026), 'FATURADO', 11398.80, 'Equipamentos para a nova filial.');
INSERT INTO pedido
    (pedido_id, cliente_id, usuario_id, data_pedido, status, valor_total, observacao)
VALUES
    (2, 2, 2, MDY(6, 12, 2026), 'CONFIRMADO', 4846.50, 'Renovação do parque de monitores.');
INSERT INTO pedido
    (pedido_id, cliente_id, usuario_id, data_pedido, status, valor_total, observacao)
VALUES
    (3, 3, 1, MDY(6, 20, 2026), 'ABERTO', 1969.50, 'Aguardando aprovação financeira.');
INSERT INTO pedido
    (pedido_id, cliente_id, usuario_id, data_pedido, status, valor_total, observacao)
VALUES
    (4, 1, 2, MDY(6, 25, 2026), 'CANCELADO', 649.90, 'Cliente solicitou cancelamento.');

INSERT INTO item_pedido
    (item_pedido_id, pedido_id, produto_id, quantidade, preco_unitario, desconto, valor_total)
VALUES
    (1, 1, 1, 2, 4899.90, 0, 9799.80);
INSERT INTO item_pedido
    (item_pedido_id, pedido_id, produto_id, quantidade, preco_unitario, desconto, valor_total)
VALUES
    (2, 1, 2, 1, 1599.00, 0, 1599.00);
INSERT INTO item_pedido
    (item_pedido_id, pedido_id, produto_id, quantidade, preco_unitario, desconto, valor_total)
VALUES
    (3, 2, 2, 3, 1599.00, 0, 4797.00);
INSERT INTO item_pedido
    (item_pedido_id, pedido_id, produto_id, quantidade, preco_unitario, desconto, valor_total)
VALUES
    (4, 2, 4, 1, 49.90, 0.40, 49.50);
INSERT INTO item_pedido
    (item_pedido_id, pedido_id, produto_id, quantidade, preco_unitario, desconto, valor_total)
VALUES
    (5, 3, 5, 4, 329.90, 0, 1319.60);
INSERT INTO item_pedido
    (item_pedido_id, pedido_id, produto_id, quantidade, preco_unitario, desconto, valor_total)
VALUES
    (6, 3, 3, 1, 649.90, 0, 649.90);
INSERT INTO item_pedido
    (item_pedido_id, pedido_id, produto_id, quantidade, preco_unitario, desconto, valor_total)
VALUES
    (7, 4, 3, 1, 649.90, 0, 649.90);

COMMIT WORK;
