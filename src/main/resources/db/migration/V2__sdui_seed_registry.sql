-- Registry seed: bounds v1, closed behavior vocabulary, 18 component types.
-- Kept identical to packages/sdui/src/core/registry.ts (contract test: RegistryContractTest).

INSERT INTO sdui_bounds (bounds_version, max_depth, max_nodes, max_string_length, max_props, max_children, max_array_items, max_identifier_length, max_chain_depth)
VALUES (1, 32, 5000, 4096, 64, 256, 512, 128, 4);

INSERT INTO sdui_behavior (behavior, required_fields, policy_check, description) VALUES
  ('navigate',     '["target"]',    'none',   'Pindah ke pageKey lain (target) + params'),
  ('back',         '[]',            'none',   'Kembali ke halaman sebelumnya'),
  ('submit',       '[]',            'none',   'Kirim nilai form ke aplikasi / backend'),
  ('externalLink', '["href"]',      'domain', 'Buka href (wajib lolos allowedDomains)'),
  ('openModal',    '["modalId"]',   'none',   'Buka modal'),
  ('closeModal',   '[]',            'none',   'Tutup modal'),
  ('toast',        '[]',            'none',   'Tampilkan pesan singkat'),
  ('customEvent',  '["eventName"]', 'event',  'Kirim event bernama (wajib lolos allowedEvents)'),
  ('api',          '["url"]',       'domain', 'Panggil API (url wajib lolos allowedDomains) + onSuccess/onError');

INSERT INTO sdui_component_type (component_type, category, accepts_children, default_props, allowed_behaviors, props_schema) VALUES
  ('Container', 'layout',     TRUE,  '{"as":"div"}', '[]', '{}'),
  ('Flex',      'layout',     TRUE,  '{"direction":"row","gap":"md","justify":"start"}', '[]',
     '{"properties":{"direction":{"enum":["row","column"]},"justify":{"enum":["start","center","end","between"]}}}'),
  ('Grid',      'layout',     TRUE,  '{"columns":2,"gap":"lg"}', '[]', '{"properties":{"columns":{"type":"integer","minimum":1,"maximum":12}}}'),
  ('Card',      'layout',     TRUE,  '{}', '[]', '{}'),
  ('Hero',      'layout',     TRUE,  '{}', '[]', '{}'),
  ('Space',     'layout',     FALSE, '{"height":"lg"}', '[]', '{}'),
  ('Divider',   'layout',     FALSE, '{}', '[]', '{}'),
  ('Heading',   'content',    FALSE, '{"content":"Heading","level":2}', '[]', '{}'),
  ('Text',      'content',    FALSE, '{"content":"Text","tag":"p"}', '[]', '{}'),
  ('Image',     'content',    FALSE, '{"src":"","alt":"","loading":"lazy"}', '[]', '{}'),
  ('Form',      'form',       TRUE,  '{}', '["submit","api","navigate","toast"]', '{}'),
  ('Input',     'form',       FALSE, '{"name":"field","label":"Field","type":"text"}', '[]', '{}'),
  ('Select',    'form',       FALSE, '{"name":"choice","label":"Choice","options":[]}', '[]', '{}'),
  ('Textarea',  'form',       FALSE, '{"name":"notes","label":"Notes","rows":4}', '[]', '{}'),
  ('Button',    'navigation', FALSE, '{"label":"Button","type":"button"}',
     '["navigate","back","submit","externalLink","openModal","closeModal","toast","customEvent","api"]', '{}'),
  ('Link',      'navigation', FALSE, '{"label":"Link","href":"#"}',
     '["navigate","back","submit","externalLink","openModal","closeModal","toast","customEvent","api"]', '{}'),
  ('Toast',     'feedback',   TRUE,  '{"message":"Toast"}', '[]', '{}'),
  ('Modal',     'feedback',   TRUE,  '{"title":"Modal"}', '[]', '{}');
